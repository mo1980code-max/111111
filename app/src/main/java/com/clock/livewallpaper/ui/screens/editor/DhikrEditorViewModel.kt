package com.clock.livewallpaper.ui.screens.editor

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clock.livewallpaper.data.DhikrRepository
import com.clock.livewallpaper.data.DhikrSaveResult
import com.clock.livewallpaper.data.local.DhikrCategory
import com.clock.livewallpaper.ui.navigation.Routes
import com.clock.livewallpaper.widget.WidgetRefresh
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Validation failures are shown inline; nothing is silently dropped or auto-corrected. */
enum class EditorError { NONE, BLANK_TEXT, INVALID_REPEAT }

data class EditorUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    /** True for bundled, verified dhikr: the text is read-only, only the switches can change. */
    val isProtected: Boolean = false,
    val text: String = "",
    val repeatCount: Int = 1,
    val category: DhikrCategory = DhikrCategory.CUSTOM,
    val isEnabled: Boolean = true,
    val includeInOverlay: Boolean = true,
    val error: EditorError = EditorError.NONE,
    val canDelete: Boolean = false
)

@HiltViewModel
class DhikrEditorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val repository: DhikrRepository
) : ViewModel() {

    private val dhikrId: Long =
        savedStateHandle.get<Long>(Routes.ARG_DHIKR_ID) ?: Routes.NEW_DHIKR_ID

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<EditorEvent>(
        replay = 0,
        extraBufferCapacity = 2,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<EditorEvent> = _events

    init {
        viewModelScope.launch {
            val existing = if (dhikrId > 0) repository.byId(dhikrId) else null
            _state.value = if (existing == null) {
                EditorUiState(loading = false, isNew = true)
            } else {
                EditorUiState(
                    loading = false,
                    isNew = false,
                    isProtected = existing.isDefault,
                    text = existing.arabicText,
                    repeatCount = existing.repeatCount,
                    category = DhikrCategory.fromKey(existing.category) ?: DhikrCategory.CUSTOM,
                    isEnabled = existing.isEnabled,
                    includeInOverlay = existing.includeInOverlay,
                    canDelete = !existing.isDefault
                )
            }
        }
    }

    fun onTextChange(value: String) {
        _state.update { it.copy(text = value, error = EditorError.NONE) }
    }

    fun onRepeatChange(value: Int) {
        _state.update {
            it.copy(
                repeatCount = value.coerceIn(DhikrRepository.MIN_REPEAT, DhikrRepository.MAX_REPEAT),
                error = EditorError.NONE
            )
        }
    }

    fun onCategoryChange(category: DhikrCategory) {
        _state.update { it.copy(category = category) }
    }

    fun onEnabledChange(enabled: Boolean) {
        _state.update { it.copy(isEnabled = enabled) }
    }

    fun onOverlayChange(include: Boolean) {
        _state.update { it.copy(includeInOverlay = include) }
    }

    fun save() {
        val current = _state.value
        viewModelScope.launch {
            val result = repository.save(
                id = if (dhikrId > 0) dhikrId else null,
                text = current.text,
                repeatCount = current.repeatCount,
                category = current.category,
                isEnabled = current.isEnabled,
                includeInOverlay = current.includeInOverlay
            )
            when (result) {
                is DhikrSaveResult.Saved -> {
                    WidgetRefresh.request(context)
                    _events.tryEmit(EditorEvent.Saved)
                }
                DhikrSaveResult.BlankText ->
                    _state.update { it.copy(error = EditorError.BLANK_TEXT) }
                DhikrSaveResult.InvalidRepeat ->
                    _state.update { it.copy(error = EditorError.INVALID_REPEAT) }
            }
        }
    }

    fun delete() {
        if (dhikrId <= 0) return
        viewModelScope.launch {
            if (repository.deleteUserDhikr(dhikrId)) {
                WidgetRefresh.request(context)
                _events.tryEmit(EditorEvent.Deleted)
            }
        }
    }
}

sealed interface EditorEvent {
    data object Saved : EditorEvent
    data object Deleted : EditorEvent
}
