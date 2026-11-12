package io.github.dimitrysaf.asko.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.window.core.layout.WindowSizeClass
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.components.EmptyState

import io.github.dimitrysaf.asko.pages.exercises.CustomExerciseEditorPage
import io.github.dimitrysaf.asko.pages.stats.ExerciseStatsPage
import io.github.dimitrysaf.asko.pages.stats.StatsPage
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
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
 * App shell: the tabs with their navigation bar (see [TabsPage]), and the pages opened over them,
 * which cover the bar. [open] is a page a launcher shortcut asked for: it opens over the Routines
 * tab, then [onOpened].
 */
@Composable
fun AskoNavHost(open: NavKey? = null, onOpened: () -> Unit = {}) {
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

    LaunchedEffect(open) {
        if (open == null) return@LaunchedEffect
        if (backStack.lastOrNull() != open) {
            tabDirection = 1
            backStack.clear()
            backStack.add(RoutinesRoute)
            backStack.add(open)
        }
        onOpened()
    }

    // What NavDisplay shows: the tabs as one page, then the pages opened over them. Pages deeper
    // than a tab slide in over the whole tab page, navigation bar and all, and slide off it again;
    // the bar never moves while they do.
    val pages: List<NavKey> = listOf(TabsRoute) + backStack.drop(1)

    // The tab page: the navigation bar (or rail) and the current tab.
    val tabsPage: @Composable () -> Unit = {
        TabsPage(
            root = root ?: RoutinesRoute,
            direction = tabDirection,
            isCurrentTab = ::isCurrentTab,
            onTab = { destination -> if (!isCurrentTab(destination)) switchRoot(destination.route) },
        ) { tab ->
            when (tab) {
                is RoutinesRoute -> {
                    RoutinesPage(
                        onDayClick = { day ->
                            switchRoot(HistoryRoute(day.toString()))
                        },
                        onLog = { backStack.add(LogWorkoutRoute) },
                        onCreateRoutine = { name -> backStack.add(RoutineEditorRoute(name = name)) },
                        onRoutineClick = { id -> backStack.add(RoutineRoute(id)) },
                        onResumeWorkout = { id -> backStack.add(ActiveWorkoutRoute(id)) },
                        onStartRoutine = { id -> backStack.add(ActiveWorkoutRoute(id)) },
                        onSettings = { backStack.add(SettingsRoute) },
                        onStats = { backStack.add(StatsRoute) },
                    )
                }
                is DiscoveryRoute -> {
                    DiscoveryPage(
                        onExerciseClick = { id -> backStack.add(ExerciseDetailRoute(id)) },
                        onCreateExercise = { name -> backStack.add(CustomExerciseEditorRoute(name = name)) },
                    )
                }
                is HistoryRoute -> {
                    HistoryPage(
                        initialDay = tab.day?.let(LocalDate::parse),
                        onWorkoutClick = { id -> backStack.add(WorkoutRoute(id)) },
                    )
                }
                else -> {}
            }
        }
    }
    val provider = entryProvider<NavKey> {
        entry<TabsRoute> { tabsPage() }
        entry<RoutineRoute> { key ->
            RoutinePage(
                routineId = key.routineId,
                onBack = goBack,
                onEdit = { backStack.add(RoutineEditorRoute(key.routineId)) },
                onOpenRoutine = { id -> backStack.add(RoutineRoute(id)) },
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
                onOpenStats = { id -> backStack.add(ExerciseStatsRoute(id)) },
                onEdit = { id -> backStack.add(CustomExerciseEditorRoute(id)) },
                onDeleted = { if (backStack.lastOrNull() == key) backStack.removeLastOrNull() },
            )
        }
        entry<CustomExerciseEditorRoute> { key ->
            CustomExerciseEditorPage(
                exerciseId = key.exerciseId,
                initialName = key.name,
                onBack = goBack,
                // Made: the editor becomes its page. Changed: back to its page.
                onSaved = { id ->
                    if (backStack.lastOrNull() == key) backStack.removeLastOrNull()
                    if (backStack.lastOrNull() != ExerciseDetailRoute(id)) backStack.add(ExerciseDetailRoute(id))
                },
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
    }

    // Wide windows (tablets, unfolded phones): the tab stays on the left and what it opens shows
    // beside it, one page at a time, sliding as on a phone; nothing open yet says so.
    val wide = currentWindowAdaptiveInfo().windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
    if (wide) {
        Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            Box(Modifier.weight(ListPaneWeight).fillMaxHeight()) { tabsPage() }
            Box(Modifier.weight(1f - ListPaneWeight).fillMaxHeight()) {
                val details = backStack.drop(1)
                if (details.isEmpty()) {
                    EmptyState(painterResource(R.drawable.ic_fitness), stringResource(R.string.pane_empty))
                } else {
                    NavDisplay(
                        backStack = details,
                        onBack = { goBack() },
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                        transitionSpec = slidePushTransitionSpec(),
                        popTransitionSpec = slidePopTransitionSpec(),
                        predictivePopTransitionSpec = slidePredictivePopTransitionSpec(),
                        entryProvider = provider,
                    )
                }
            }
        }
        return
    }

    NavDisplay(
        backStack = pages,
        onBack = { goBack() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(), // one ViewModel per back-stack entry
        ),
        // Slides only, quick, no fading: new pages from the right, back to the right (following
        // the finger with the back gesture). Tabs slide sideways inside the tab page.
        transitionSpec = slidePushTransitionSpec(),
        popTransitionSpec = slidePopTransitionSpec(),
        predictivePopTransitionSpec = slidePredictivePopTransitionSpec(),
        entryProvider = provider,
    )
}

/** How much of a wide window the tab takes; what it opens gets the rest. */
private const val ListPaneWeight = 0.45f

/**
 * The tabs as one page: NavigationSuiteScaffold (the expressive short navigation bar on phones, the
 * wide rail on tablets) around the current tab's page ([root]), which slides in from the side its
 * tab sits on ([direction] 1: from the right). Each tab keeps its scroll position and such.
 */
@Composable
private fun TabsPage(
    root: NavKey,
    direction: Int,
    isCurrentTab: (TopLevelDestination) -> Boolean,
    onTab: (TopLevelDestination) -> Unit,
    content: @Composable (NavKey) -> Unit,
) {
    val states = rememberSaveableStateHolder()
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                item(
                    selected = isCurrentTab(destination),
                    onClick = { onTab(destination) },
                    icon = { Icon(destination.icon(), contentDescription = null) },
                    label = { Text(stringResource(destination.label)) },
                )
            }
        },
        layoutType = NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfo()),
    ) {
        AnimatedContent(
            targetState = root,
            transitionSpec = { tabSlide(direction) },
            label = "tab",
        ) { tab ->
            states.SaveableStateProvider(tab.toString()) { content(tab) }
        }
    }
}
