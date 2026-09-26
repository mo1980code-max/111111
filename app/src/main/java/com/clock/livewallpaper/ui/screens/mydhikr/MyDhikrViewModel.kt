package com.clock.livewallpaper.ui.screens.mydhikr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clock.livewallpaper.data.DhikrRepository
import com.clock.livewallpaper.data.local.DhikrEntity
import com.clock.livewallpaper.widget.WidgetRefresh
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyDhikrUiState(
    val loading: Boolean = true,
    val items: List<DhikrEntity> = emptyList()
)

@HiltViewModel
class MyDhikrViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DhikrRepository
) : ViewModel() {

    private val _deleted = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val deleted: SharedFlow<Unit> = _deleted

    val state: StateFlow<MyDhikrUiState> = repository.observeUserDhikr()
        .map { items -> MyDhikrUiState(loading = false, items = items) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MyDhikrUiState()
        )

    fun setEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnabled(id, enabled)
            WidgetRefresh.request(context)
        }
    }

    fun setIncludeInOverlay(id: Long, include: Boolean) {
        viewModelScope.launch {
            repository.setIncludeInOverlay(id, include)
            WidgetRefresh.request(context)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            if (repository.deleteUserDhikr(id)) {
                _deleted.tryEmit(Unit)
                WidgetRefresh.request(context)
            }
        }
    }
}
