package io.github.dimitrysaf.asko.pages.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.repository.ExerciseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.io.IOException

sealed interface ExerciseDetailState {
    data object Loading : ExerciseDetailState
    data object NotFound : ExerciseDetailState
    data class Loaded(val exercise: Exercise, val similar: List<Exercise>) : ExerciseDetailState
}

class ExerciseDetailViewModel(
    exerciseId: String,
    exercises: ExerciseRepository,
) : ViewModel() {
    val state: StateFlow<ExerciseDetailState> =
        flow {
            val loaded = try {
                exercises.byId(exerciseId)?.let { ExerciseDetailState.Loaded(it, exercises.similarTo(it)) }
            } catch (e: IOException) {
                null // library never downloaded and the phone is offline
            }
            emit(loaded ?: ExerciseDetailState.NotFound)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, ExerciseDetailState.Loading)
}
