package io.github.dimitrysaf.asko.pages.discovery

import io.github.dimitrysaf.asko.data.model.DownloadProgress
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.repository.ExerciseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _state = MutableStateFlow<DiscoveryState>(DiscoveryState.Loading)
    val state: StateFlow<DiscoveryState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = DiscoveryState.Loading
        viewModelScope.launch {
            _state.value = try {
                DiscoveryState.Loaded(exercises.all())
            } catch (e: IOException) {
                DiscoveryState.Failed
            }
        }
    }
}
