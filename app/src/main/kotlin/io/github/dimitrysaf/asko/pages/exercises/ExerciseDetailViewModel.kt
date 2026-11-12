package io.github.dimitrysaf.asko.pages.exercises

import io.github.dimitrysaf.asko.data.repository.CustomExerciseRepository
import android.net.Uri
import io.github.dimitrysaf.asko.data.model.ExerciseMedia
import io.github.dimitrysaf.asko.data.repository.MediaRepository
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.repository.ExerciseRepository
import io.github.dimitrysaf.asko.data.repository.FavouritesRepository
import io.github.dimitrysaf.asko.data.repository.WorkoutRepository
import io.github.dimitrysaf.asko.data.repository.asShown
import io.github.dimitrysaf.asko.data.repository.lookupsOrEmpty
import io.github.dimitrysaf.asko.data.model.Workout
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
    private val exerciseId: String,
    exercises: ExerciseRepository,
    private val favourites: FavouritesRepository,
    workouts: WorkoutRepository,
    private val media: MediaRepository,
    private val customExercises: CustomExerciseRepository,
) : ViewModel() {
    /** Deletes an exercise of yours from the library (routines and workouts keep showing it), then [then]. */
    fun retire(id: String, then: () -> Unit) {
        viewModelScope.launch {
            customExercises.retire(id)
            then()
        }
    }

    /** Adds a photo or video from the picker to the exercise (as shown, [shownId]). */
    fun addMedia(shownId: String, uri: Uri) {
        viewModelScope.launch {
            try {
                media.add(shownId, uri)
            } catch (e: IOException) {
                // The file couldn't be read: nothing is added.
            }
        }
    }

    fun removeMedia(item: ExerciseMedia) {
        viewModelScope.launch { media.remove(item) }
    }

    /**
     * Every finished workout, newest first, each exercise under the id of its version shown (so
     * its versions in each library count as one), for the charts; null until read.
     */
    val history: StateFlow<List<Workout>?> = combine(workouts.history, exercises.lookupsOrEmpty()) { history, lookup -> history.asShown(lookup) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** It's starred as a favourite. */
    val favourite: StateFlow<Boolean> =
        favourites.ids.map { exerciseId in it }.stateIn(viewModelScope, SharingStarted.Eagerly, exerciseId in favourites.ids.value)

    fun setFavourite(on: Boolean) = favourites.setFavourite(exerciseId, on)

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
