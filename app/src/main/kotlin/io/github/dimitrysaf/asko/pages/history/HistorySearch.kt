package io.github.dimitrysaf.asko.pages.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.components.EmptyState
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.model.Exercise
import io.github.dimitrysaf.asko.data.model.Note
import io.github.dimitrysaf.asko.data.model.Workout
import io.github.dimitrysaf.asko.lib.time.localDate
import kotlinx.coroutines.launch

/**
 * History's search: Material's full-screen search bar, over the tab. It finds workouts by their
 * routine's name, the exercises done and their notes (every word typed must be there), shown as
 * History shows them: under their day, newest first. Tap one to open it.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun HistorySearch(
    state: SearchBarState,
    history: List<Workout>,
    library: Map<String, Exercise>,
    notes: Map<Long, List<Note>>,
    onWorkoutClick: (workoutId: Long) -> Unit,
) {
    val text = rememberTextFieldState()
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val unnamed = stringResource(R.string.history_workout)
    val query = text.text.toString()
    val results = remember(query, history, library, notes) { history.matching(query, library, notes, unnamed) }
    ExpandedFullScreenSearchBar(
        state = state,
        inputField = {
            SearchBarDefaults.InputField(
                textFieldState = text,
                searchBarState = state,
                onSearch = { keyboard?.hide() },
                placeholder = { Text(stringResource(R.string.history_search)) },
                leadingIcon = {
                    IconButton(onClick = { scope.launch { state.animateToCollapsed() } }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                trailingIcon = if (query.isEmpty()) null else {
                    {
                        IconButton(onClick = { text.clearText() }) { Icon(Icons.Filled.Close, stringResource(R.string.action_clear_search)) }
                    }
                },
            )
        },
    ) {
        when {
            query.isBlank() -> Unit
            results.isEmpty() -> EmptyState(rememberVectorPainter(Icons.Filled.Search), stringResource(R.string.history_no_results))
            else -> LazyColumn(
                contentPadding = PaddingValues(Dimens.PageGutter),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
            ) {
                results.groupBy { it.startedAt.localDate() }.forEach { (day, workouts) ->
                    item(key = "day-$day") { DateSeparator(day) }
                    item(key = "workouts-$day") {
                        Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                            workouts.forEachIndexed { index, workout ->
                                HistoryEntry(workout, index, workouts.size, onClick = {
                                    // Out of the way at once, so the workout slides in over History.
                                    scope.launch {
                                        state.snapTo(0f)
                                        onWorkoutClick(workout.id)
                                    }
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The workouts (newest first, as given) whose routine name, exercises and notes hold every word of
 * [query], in any case; none for an empty query. [unnamed] is what a workout without a routine is called.
 */
internal fun List<Workout>.matching(
    query: String,
    library: Map<String, Exercise>,
    notes: Map<Long, List<Note>>,
    unnamed: String,
): List<Workout> {
    val words = query.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return emptyList()
    return filter { workout ->
        val text = buildList {
            add(workout.routineName ?: unnamed)
            workout.exercises.forEach { e -> library[e.exerciseId]?.name?.let(::add) }
            notes[workout.id].orEmpty().forEach { note -> addAll(listOfNotNull(note.text, note.trainer, note.gym, note.bodyPart)) }
        }.joinToString("\n").lowercase()
        words.all { it in text }
    }
}
