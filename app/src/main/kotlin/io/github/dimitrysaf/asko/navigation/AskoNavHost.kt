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
import io.github.dimitrysaf.asko.pages.stats.StatsTilePage
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
import kotlin.math.ceil
import kotlin.math.floor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.key
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import kotlin.math.abs
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
    // The system back: never past the tab.
    val goBack: () -> Unit = { if (backStack.size > 1) backStack.removeLastOrNull() }

    // Only the page on top acts. A page still sliding in or out keeps getting taps, so without
    // this a quick second tap opened a page twice, back pressed twice popped two pages (or the
    // tab itself), and a page sliding away could open pages of its own.
    fun NavKey.isTop() = backStack.lastOrNull() == this
    fun NavKey.open(route: NavKey) {
        if (isTop()) backStack.add(route)
    }
    fun NavKey.back() {
        if (isTop() && backStack.size > 1) backStack.removeLastOrNull()
    }

    // A new tab (root page); the tab strip slides to it.
    fun switchRoot(route: NavKey) {
        backStack.clear()
        backStack.add(route)
    }

    LaunchedEffect(open) {
        if (open == null) return@LaunchedEffect
        if (backStack.lastOrNull() != open) {
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
            isCurrentTab = ::isCurrentTab,
            onTab = { destination -> if (!isCurrentTab(destination)) switchRoot(destination.route) },
        ) { tab ->
            // The tab acts only while it's the one shown and nothing is open over it.
            fun open(route: NavKey) {
                if (backStack.size == 1 && backStack.firstOrNull() == tab) backStack.add(route)
            }
            fun switchFromTab(route: NavKey) {
                if (backStack.size == 1 && backStack.firstOrNull() == tab) switchRoot(route)
            }
            when (tab) {
                is RoutinesRoute -> {
                    RoutinesPage(
                        onDayClick = { day ->
                            switchFromTab(HistoryRoute(day.toString()))
                        },
                        onLog = { open(LogWorkoutRoute) },
                        onCreateRoutine = { name -> open(RoutineEditorRoute(name = name)) },
                        onRoutineClick = { id -> open(RoutineRoute(id)) },
                        onResumeWorkout = { id -> open(ActiveWorkoutRoute(id)) },
                        onStartRoutine = { id -> open(ActiveWorkoutRoute(id)) },
                        onSettings = { open(SettingsRoute) },
                        onStats = { open(StatsRoute) },
                    )
                }
                is DiscoveryRoute -> {
                    DiscoveryPage(
                        onExerciseClick = { id -> open(ExerciseDetailRoute(id)) },
                        onCreateExercise = { name -> open(CustomExerciseEditorRoute(name = name)) },
                    )
                }
                is HistoryRoute -> {
                    HistoryPage(
                        initialDay = tab.day?.let(LocalDate::parse),
                        onWorkoutClick = { id -> open(WorkoutRoute(id)) },
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
                onBack = { key.back() },
                onEdit = { key.open(RoutineEditorRoute(key.routineId)) },
                onOpenRoutine = { id -> key.open(RoutineRoute(id)) },
                onStart = { key.open(ActiveWorkoutRoute(key.routineId)) },
                onResumeWorkout = { id -> key.open(ActiveWorkoutRoute(id)) },
                onExerciseClick = { id -> key.open(ExerciseDetailRoute(id)) },
            )
        }
        entry<RoutineEditorRoute> { key ->
            RoutineEditorPage(
                routineId = key.routineId,
                initialName = key.name,
                onBack = { key.back() },
                onSaved = { id ->
                    // Editing: back to the routine. Creating: the editor becomes the new routine's page.
                    if (key.isTop()) {
                        backStack.removeLastOrNull()
                        if (backStack.lastOrNull() != RoutineRoute(id)) backStack.add(RoutineRoute(id))
                    }
                },
            )
        }
        entry<WorkoutFinishedRoute> { key ->
            WorkoutFinishedPage(
                workoutId = key.workoutId,
                routineName = key.routineName,
                onDone = { key.back() },
                onHistory = { day ->
                    if (key.isTop()) switchRoot(HistoryRoute(day.toString()))
                },
                onExerciseClick = { id -> key.open(ExerciseDetailRoute(id)) },
            )
        }
        entry<StatsRoute> { key ->
            StatsPage(
                onBack = { key.back() },
                onExerciseClick = { id -> key.open(ExerciseStatsRoute(id)) },
                onOpenTile = { key.open(StatsTileRoute(it)) },
            )
        }
        entry<StatsTileRoute> { key ->
            StatsTilePage(tile = key.tile, onBack = { key.back() }, onExerciseClick = { id -> key.open(ExerciseStatsRoute(id)) })
        }
        entry<ExerciseStatsRoute> { key ->
            ExerciseStatsPage(
                exerciseId = key.exerciseId,
                onBack = { key.back() },
                onOpenExercise = { key.open(ExerciseDetailRoute(key.exerciseId)) },
            )
        }
        entry<SettingsRoute> { key ->
            SettingsPage(onBack = { key.back() })
        }
        entry<WorkoutEditRoute> { key ->
            LogWorkoutPage(
                workoutId = key.workoutId,
                onBack = { key.back() },
                // Saved: back to the workout, which shows the change.
                onLogged = { if (backStack.lastOrNull() == key) backStack.removeLastOrNull() },
            )
        }
        entry<LogWorkoutRoute> { key ->
            LogWorkoutPage(
                onBack = { key.back() },
                // Logged: show it in History on its day.
                onLogged = { day ->
                    if (key.isTop()) switchRoot(HistoryRoute(day.toString()))
                },
            )
        }
        entry<WorkoutRoute> { key ->
            WorkoutPage(
                workoutId = key.workoutId,
                onBack = { key.back() },
                onEdit = { key.open(WorkoutEditRoute(key.workoutId)) },
                onExerciseClick = { id -> key.open(ExerciseDetailRoute(id)) },
            )
        }
        entry<ExerciseDetailRoute> { key ->
            ExerciseDetailPage(
                exerciseId = key.exerciseId,
                onBack = { key.back() },
                onExerciseClick = { id -> key.open(ExerciseDetailRoute(id)) },
                onOpenStats = { id -> key.open(ExerciseStatsRoute(id)) },
                onEdit = { id -> key.open(CustomExerciseEditorRoute(id)) },
                onDeleted = { if (backStack.lastOrNull() == key) backStack.removeLastOrNull() },
            )
        }
        entry<CustomExerciseEditorRoute> { key ->
            CustomExerciseEditorPage(
                exerciseId = key.exerciseId,
                initialName = key.name,
                onBack = { key.back() },
                // Made: the editor becomes its page. Changed: back to its page.
                onSaved = { id ->
                    if (key.isTop()) {
                        backStack.removeLastOrNull()
                        if (backStack.lastOrNull() != ExerciseDetailRoute(id)) backStack.add(ExerciseDetailRoute(id))
                    }
                },
            )
        }
        entry<ActiveWorkoutRoute> { key ->
            ActiveWorkoutPage(
                routineId = key.routineId,
                onBack = { key.back() },
                onExerciseClick = { id -> key.open(ExerciseDetailRoute(id)) },
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

/** Whether the tab this is in is the one shown (false while it waits, hidden, behind another). */
val LocalTabShown = compositionLocalOf { true }

/**
 * The tabs as one page: NavigationSuiteScaffold (the expressive short navigation bar on phones, the
 * wide rail on tablets) around the tabs, laid side by side on one strip in the bar's order, like
 * a pager. Switching tab moves the strip to it ([root]): the pane slides left or right, passing
 * any tab between. However fast you switch, it's the one strip turning round, never pages on top
 * of each other; a quick switch there and back barely moves it.
 *
 * A tab is built the first time it comes into view and then kept (off screen it isn't placed,
 * so not drawn and not touchable), so switching rebuilds nothing.
 */
@Composable
private fun TabsPage(
    root: NavKey,
    isCurrentTab: (TopLevelDestination) -> Boolean,
    onTab: (TopLevelDestination) -> Unit,
    content: @Composable (NavKey) -> Unit,
) {
    val states = rememberSaveableStateHolder()
    val tabs = TopLevelDestination.entries
    fun indexOf(route: NavKey) = tabs.indexOfFirst { it.route::class == route::class }.coerceAtLeast(0)
    val target = indexOf(root)
    // Where the strip is, in tabs: 0 shows the first tab, 1.5 half of the second and the third.
    val position = remember { Animatable(target.toFloat()) }
    LaunchedEffect(target) { position.animateTo(target.toFloat(), TabSlideSpec) }
    // Each tab's latest route (History may be opened on a day), once it has come into view.
    val routes = remember { mutableStateMapOf<Int, NavKey>() }
    if (routes[target] != root) routes[target] = root
    val inView by remember { derivedStateOf { floor(position.value).toInt()..ceil(position.value).toInt() } }
    for (i in inView) if (i !in routes) routes[i] = tabs[i].route
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            tabs.forEach { destination ->
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
        Box(Modifier.fillMaxSize().clipToBounds()) {
            routes.keys.sorted().forEach { i ->
                val route = routes.getValue(i)
                key(i) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, placeable.height) {
                                    // Its place on the strip, relative to where the strip is.
                                    val shift = i - position.value
                                    if (abs(shift) < 1f) {
                                        placeable.placeWithLayer(0, 0) { translationX = shift * placeable.width }
                                    }
                                }
                            },
                    ) {
                        CompositionLocalProvider(LocalTabShown provides (i == target)) {
                            key(route) {
                                states.SaveableStateProvider(route.toString()) { content(route) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The strip's slide: a spring with no bounce, about as quick as a page's slide. A spring, so a
 * switch made mid-slide carries on from the strip's speed instead of jolting.
 */
private val TabSlideSpec = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 500f)
