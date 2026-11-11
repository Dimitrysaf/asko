package io.github.dimitrysaf.asko.pages.discovery

import io.github.dimitrysaf.asko.data.model.DownloadProgress
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.repository.ExerciseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.IOException

sealed interface DiscoveryState {
    data object Loading : DiscoveryState
    /** First download failed, usually because the phone is offline. */
    data object Failed : DiscoveryState
    data class Loaded(val exercises: List<Exercise>) : DiscoveryState
}

class DiscoveryViewModel(private val exercises: ExerciseRepository) : ViewModel() {
    /** The library's first download, while it runs: the loading screen shows it. */
    val download: StateFlow<DownloadProgress?> = exercises.downloadProgress

    // Opened again: straight from the cache, no loading.
    private val _state = MutableStateFlow<DiscoveryState>(
        exercises.browsableNow()?.let { DiscoveryState.Loaded(it) } ?: DiscoveryState.Loading,
    )
    val state: StateFlow<DiscoveryState> = _state.asStateFlow()

    init {
        load()
    }

    private var loading: Job? = null

    /** The libraries as Settings has them (it follows changes there); Failed when none can be read. */
    fun load() {
        loading?.cancel()
        if (_state.value !is DiscoveryState.Loaded) _state.value = DiscoveryState.Loading
        loading = viewModelScope.launch {
            exercises.browsable
                .catch { e -> if (e is IOException) _state.value = DiscoveryState.Failed else throw e }
                .collect { _state.value = DiscoveryState.Loaded(it) }
        }
    }
}
