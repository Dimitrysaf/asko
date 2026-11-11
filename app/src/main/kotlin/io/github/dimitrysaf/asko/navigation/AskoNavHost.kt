package io.github.dimitrysaf.asko.navigation

import io.github.dimitrysaf.asko.pages.stats.ExerciseStatsPage
import io.github.dimitrysaf.asko.pages.stats.StatsPage
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.dimitrysaf.asko.pages.discovery.DiscoveryPage
import io.github.dimitrysaf.asko.pages.exercises.ExerciseDetailPage
import io.github.dimitrysaf.asko.pages.history.HistoryPage
import io.github.dimitrysaf.asko.pages.routines.RoutineEditorPage
import io.github.dimitrysaf.asko.pages.routines.RoutinePage
import io.github.dimitrysaf.asko.pages.routines.RoutinesPage
import io.github.dimitrysaf.asko.pages.workout.ActiveWorkoutPage
import io.github.dimitrysaf.asko.pages.workout.LogWorkoutPage
import io.github.dimitrysaf.asko.pages.workout.WorkoutFinishedPage
import io.github.dimitrysaf.asko.pages.workout.WorkoutPage
import io.github.dimitrysaf.asko.pages.settings.SettingsPage
import java.time.LocalDate

/**
 * App shell. NavigationSuiteScaffold picks the expressive short navigation bar on phones
 * and the wide navigation rail on tablets; it hides on pages deeper than a tab.
 */
@Composable
fun AskoNavHost() {
    val backStack = rememberNavBackStack(RoutinesRoute)
    val root = backStack.firstOrNull()
    // Tabs match by page type, so History opened on a day (HistoryRoute("2026-11-10")) still highlights its tab.
    fun isCurrentTab(destination: TopLevelDestination) = root != null && root::class == destination.route::class
    val goBack: () -> Unit = { backStack.removeLastOrNull() }

    // Which way the pages slide when the tab (the root page) changes: towards the new tab's side.
    var tabDirection by remember { mutableIntStateOf(1) }
    fun tabIndex(route: NavKey) = TopLevelDestination.entries.indexOfFirst { it.route::class == route::class }
    fun switchRoot(route: NavKey) {
        tabDirection = if (tabIndex(route) >= (root?.let(::tabIndex) ?: 0)) 1 else -1
        backStack.clear()
        backStack.add(route)
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                item(
                    selected = isCurrentTab(destination),
                    onClick = {
                        if (!isCurrentTab(destination)) switchRoot(destination.route)
                    },
                    icon = { Icon(destination.icon(), contentDescription = null) },
                    label = { Text(stringResource(destination.label)) },
                )
            }
        },
        layoutType = if (backStack.size == 1) {
            NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfo())
        } else {
            NavigationSuiteType.None
        },
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { goBack() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(), // one ViewModel per back-stack entry
            ),
            // Slides only, quick, no fading: new pages from the right, back to the right (following
            // the finger with the back gesture), tabs sideways by their order.
            transitionSpec = {
                if (targetState.previousEntries.isEmpty()) {
                    slideTabTransitionSpec<NavKey>(tabDirection).invoke(this)
                } else {
                    slidePushTransitionSpec<NavKey>().invoke(this)
                }
            },
            popTransitionSpec = slidePopTransitionSpec(),
            predictivePopTransitionSpec = slidePredictivePopTransitionSpec(),
            entryProvider = entryProvider {
                entry<RoutinesRoute> {
                    RoutinesPage(
                        onDayClick = { day ->
                            switchRoot(HistoryRoute(day.toString()))
                        },
                        onLog = { backStack.add(LogWorkoutRoute) },
                        onCreateRoutine = { name -> backStack.add(RoutineEditorRoute(name = name)) },
                        onRoutineClick = { id -> backStack.add(RoutineRoute(id)) },
                        onResumeWorkout = { id -> backStack.add(ActiveWorkoutRoute(id)) },
                        onSettings = { backStack.add(SettingsRoute) },
                        onStats = { backStack.add(StatsRoute) },
                    )
                }
                entry<RoutineRoute> { key ->
                    RoutinePage(
                        routineId = key.routineId,
                        onBack = goBack,
                        onEdit = { backStack.add(RoutineEditorRoute(key.routineId)) },
                        onStart = { backStack.add(ActiveWorkoutRoute(key.routineId)) },
                        onResumeWorkout = { id -> backStack.add(ActiveWorkoutRoute(id)) },
                        onExerciseClick = { id -> backStack.add(ExerciseDetailRoute(id)) },
                    )
                }
                entry<RoutineEditorRoute> { key ->
                    RoutineEditorPage(
                        routineId = key.routineId,
                        initialName = key.name,
                        onBack = goBack,
                        onSaved = { id ->
                            // Editing: back to the routine. Creating: the editor becomes the new routine's page.
                            if (backStack.lastOrNull() == key) backStack.removeLastOrNull()
                            if (backStack.lastOrNull() != RoutineRoute(id)) backStack.add(RoutineRoute(id))
                        },
                    )
                }
                entry<WorkoutFinishedRoute> { key ->
                    WorkoutFinishedPage(
                        workoutId = key.workoutId,
                        routineName = key.routineName,
                        onDone = goBack,
                        onHistory = { day ->
                            switchRoot(HistoryRoute(day.toString()))
                        },
                        onExerciseClick = { id -> backStack.add(ExerciseDetailRoute(id)) },
                    )
                }
                entry<StatsRoute> {
                    StatsPage(onBack = goBack, onExerciseClick = { id -> backStack.add(ExerciseStatsRoute(id)) })
                }
                entry<ExerciseStatsRoute> { key ->
                    ExerciseStatsPage(
                        exerciseId = key.exerciseId,
                        onBack = goBack,
                        onOpenExercise = { backStack.add(ExerciseDetailRoute(key.exerciseId)) },
                    )
                }
                entry<SettingsRoute> {
                    SettingsPage(onBack = goBack)
                }
                entry<WorkoutEditRoute> { key ->
                    LogWorkoutPage(
                        workoutId = key.workoutId,
                        onBack = goBack,
                        // Saved: back to the workout, which shows the change.
                        onLogged = { if (backStack.lastOrNull() == key) backStack.removeLastOrNull() },
                    )
                }
                entry<LogWorkoutRoute> {
                    LogWorkoutPage(
                        onBack = goBack,
                        // Logged: show it in History on its day.
                        onLogged = { day ->
                            switchRoot(HistoryRoute(day.toString()))
                        },
                    )
                }
                entry<DiscoveryRoute> {
                    DiscoveryPage(onExerciseClick = { id -> backStack.add(ExerciseDetailRoute(id)) })
                }
                entry<HistoryRoute> { key ->
                    HistoryPage(
                        initialDay = key.day?.let(LocalDate::parse),
                        onWorkoutClick = { id -> backStack.add(WorkoutRoute(id)) },
                    )
                }
                entry<WorkoutRoute> { key ->
                    WorkoutPage(
                        workoutId = key.workoutId,
                        onBack = goBack,
                        onEdit = { backStack.add(WorkoutEditRoute(key.workoutId)) },
                        onExerciseClick = { id -> backStack.add(ExerciseDetailRoute(id)) },
                    )
                }
                entry<ExerciseDetailRoute> { key ->
                    ExerciseDetailPage(
                        exerciseId = key.exerciseId,
                        onBack = goBack,
                        onExerciseClick = { id -> backStack.add(ExerciseDetailRoute(id)) },
                    )
                }
                entry<ActiveWorkoutRoute> { key ->
                    ActiveWorkoutPage(
                        routineId = key.routineId,
                        onBack = goBack,
                        onExerciseClick = { id -> backStack.add(ExerciseDetailRoute(id)) },
                        // Finished: the workout becomes its summary; with nothing logged, just back.
                        onFinished = { id, name ->
                            if (backStack.lastOrNull() == key) {
                                backStack.removeLastOrNull()
                                if (id != null) backStack.add(WorkoutFinishedRoute(id, name))
                            }
                        },
                    )
                }
            },
        )
    }
}
