package com.clock.livewallpaper.ui.screens.reading

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clock.livewallpaper.data.DhikrRepository
import com.clock.livewallpaper.data.local.DhikrCategory
import com.clock.livewallpaper.data.local.DhikrEntity
import com.clock.livewallpaper.data.prefs.SettingsRepository
import com.clock.livewallpaper.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One row of the reading list with the repetitions the user has already completed. */
data class ReadingItem(
    val entity: DhikrEntity,
    val done: Int
) {
    val target: Int get() = entity.repeatCount.coerceAtLeast(1)
    val completed: Boolean get() = done >= target
    val remaining: Int get() = (target - done).coerceAtLeast(0)
}

data class ReadingUiState(
    val loading: Boolean = true,
    val category: DhikrCategory = DhikrCategory.MORNING,
    val items: List<ReadingItem> = emptyList(),
    val readerFontScale: Float = 1f,
    val haptic: Boolean = true
) {
    val total: Int get() = items.size
    val completedCount: Int get() = items.count { it.completed }
    val allDone: Boolean get() = items.isNotEmpty() && completedCount == total
    val progress: Float get() = if (total == 0) 0f else completedCount.toFloat() / total
    /** Index of the dhikr the user is on - the first one still unfinished. */
    val currentIndex: Int get() = items.indexOfFirst { !it.completed }
}

@HiltViewModel
class ReadingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: DhikrRepository,
    settings: SettingsRepository
) : ViewModel() {

    val category: DhikrCategory =
        DhikrCategory.fromKey(savedStateHandle.get<String>(Routes.ARG_CATEGORY))
            ?: DhikrCategory.MORNING

    /** Session progress, kept in the ViewModel so a rotation never loses the count. */
    private val counters = MutableStateFlow<Map<Long, Int>>(emptyMap())

    init {
        viewModelScope.launch { runCatching { repository.ensureSeeded() } }
    }

    val state: StateFlow<ReadingUiState> = combine(
        repository.observeCategory(category),
        counters,
        settings.appearance,
        settings.tasbeeh
    ) { rows, counts, appearance, tasbeeh ->
        ReadingUiState(
            loading = false,
            category = category,
            // A dhikr the user switched off is not part of the guided reading.
            items = rows.filter { it.isEnabled }.map { entity ->
                ReadingItem(entity = entity, done = counts[entity.id] ?: 0)
            },
            readerFontScale = appearance.readerFontScale,
            haptic = tasbeeh.haptic
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReadingUiState()
    )

    /** One tap = one repetition, never more than the dhikr asks for. */
    fun count(item: ReadingItem) {
        counters.value = counters.value.toMutableMap().apply {
            val next = ((this[item.entity.id] ?: 0) + 1).coerceAtMost(item.target)
            this[item.entity.id] = next
        }
    }

    /** Marks the whole dhikr as read without tapping through every repetition. */
    fun complete(item: ReadingItem) {
        counters.value = counters.value.toMutableMap().apply {
            this[item.entity.id] = item.target
        }
    }

    fun restart() {
        counters.value = emptyMap()
    }
}
