package io.github.dimitrysaf.asko.navigation

import io.github.dimitrysaf.asko.pages.stats.StatsTile
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import io.github.dimitrysaf.asko.R
import kotlinx.serialization.Serializable

// Every screen is a NavKey; @Serializable lets the back stack survive process death.

/** The tabs as one page under the pages opened over them (the app shell's own; never on the back stack). */
@Serializable data object TabsRoute : NavKey

@Serializable data object RoutinesRoute : NavKey

@Serializable data object DiscoveryRoute : NavKey

/** [day] (ISO date, e.g. "2026-11-10") opens History with that day selected. */
@Serializable data class HistoryRoute(val day: String? = null) : NavKey

@Serializable data class ExerciseDetailRoute(val exerciseId: String) : NavKey

/** Making ([exerciseId] empty, called [name]) or changing an exercise of yours. */
@Serializable data class CustomExerciseEditorRoute(val exerciseId: String = "", val name: String = "") : NavKey

/** Doing the routine [routineId], set by set. */
@Serializable data class ActiveWorkoutRoute(val routineId: Long) : NavKey

/** A saved workout, opened from History. */
@Serializable data class WorkoutRoute(val workoutId: Long) : NavKey

/** Statistics over all your workouts, from the Routines tab. */
@Serializable data object StatsRoute : NavKey

/** One Stats tile's page ([tile]: sets, reps, time or weight lifted), from Stats. */
@Serializable data class StatsTileRoute(val tile: StatsTile) : NavKey

/** One exercise's stats and charts ([exerciseId]), from Stats. */
@Serializable data class ExerciseStatsRoute(val exerciseId: String) : NavKey

/** The app's settings, from the Routines tab. */
@Serializable data object SettingsRoute : NavKey

/** Editing a saved workout ([workoutId]) in the log editor. */
@Serializable data class WorkoutEditRoute(val workoutId: Long) : NavKey

/** A workout just finished ([workoutId], done from the routine [routineName]): the celebration and summary. */
@Serializable data class WorkoutFinishedRoute(val workoutId: Long, val routineName: String) : NavKey

/** Log a workout done without the app: pick exercises, fill in the sets, log. */
@Serializable data object LogWorkoutRoute : NavKey

@Serializable data class RoutineRoute(val routineId: Long) : NavKey

/** [routineId] 0 creates a new routine called [name]. */
@Serializable data class RoutineEditorRoute(val routineId: Long = 0, val name: String = "") : NavKey

/** Tabs in the navigation bar (phone) / rail (tablet). */
enum class TopLevelDestination(
    val route: NavKey,
    val icon: @Composable () -> Painter,
    @param:StringRes val label: Int,
) {
    // A dumbbell for your routines, a clock turning back for what you've done.
    Routines(RoutinesRoute, { painterResource(R.drawable.ic_fitness) }, R.string.nav_routines),
    Discovery(DiscoveryRoute, { rememberVectorPainter(Icons.Filled.Search) }, R.string.nav_discovery),
    History(HistoryRoute(), { painterResource(R.drawable.ic_history) }, R.string.nav_history),
}
