package com.clock.livewallpaper.ui.screens.adhkar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clock.livewallpaper.data.DhikrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdhkarUiState(
    val loading: Boolean = true,
    val counts: Map<String, Int> = emptyMap()
)

@HiltViewModel
class AdhkarViewModel @Inject constructor(
    private val repository: DhikrRepository
) : ViewModel() {

    init {
        // Safety net: the categories screen is reachable from a widget before Home was ever opened.
        viewModelScope.launch { runCatching { repository.ensureSeeded() } }
    }

    val state: StateFlow<AdhkarUiState> = repository.observeCategoryCounts()
        .map { counts -> AdhkarUiState(loading = false, counts = counts) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AdhkarUiState()
        )
}
