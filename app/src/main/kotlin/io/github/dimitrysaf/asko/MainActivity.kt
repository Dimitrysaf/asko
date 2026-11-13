package io.github.dimitrysaf.asko

import io.github.dimitrysaf.asko.components.AppSnackbar
import io.github.dimitrysaf.asko.components.LocalAppSnackbar
import androidx.compose.runtime.remember
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation3.runtime.NavKey
import io.github.dimitrysaf.asko.lib.format.LocalWeightUnit
import io.github.dimitrysaf.asko.lib.format.LocalDistanceUnit
import io.github.dimitrysaf.asko.components.LocalExerciseMedia
import io.github.dimitrysaf.asko.components.LocalShowMuscles
import io.github.dimitrysaf.asko.lib.shortcuts.AppShortcuts
import io.github.dimitrysaf.asko.navigation.ActiveWorkoutRoute
import io.github.dimitrysaf.asko.navigation.AskoNavHost
import io.github.dimitrysaf.asko.navigation.LogWorkoutRoute
import io.github.dimitrysaf.asko.navigation.RoutineRoute
import io.github.dimitrysaf.asko.theme.AskoTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val container get() = (application as AskoApplication).container

    /** A page a launcher shortcut asked for, until the app has opened it. */
    private val opening = MutableStateFlow<NavKey?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only a fresh start: after a rotation the back stack already has the page.
        if (savedInstanceState == null) open(intent)
        setContent {
            val weightUnit by container.settings.weightUnit.collectAsStateWithLifecycle()
            val distanceUnit by container.settings.distanceUnit.collectAsStateWithLifecycle()
            val page by opening.collectAsStateWithLifecycle()
            val media by container.mediaRepository.media.collectAsStateWithLifecycle()
            val showMuscles by container.settings.showMuscles.collectAsStateWithLifecycle()
            val snackbar = remember { AppSnackbar(container.appScope) }
            AskoTheme {
                CompositionLocalProvider(LocalAppSnackbar provides snackbar, LocalWeightUnit provides weightUnit, LocalDistanceUnit provides distanceUnit, LocalExerciseMedia provides media, LocalShowMuscles provides showMuscles) {
                    AskoNavHost(open = page, onOpened = { opening.value = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        open(intent)
    }

    /** Opens the page of the shortcut [intent] came from. Resume checks the workout is still on. */
    private fun open(intent: Intent?) {
        val target = AppShortcuts.targetOf(intent) ?: return
        lifecycleScope.launch {
            opening.value = when (target) {
                AppShortcuts.Target.Resume -> container.activeWorkoutStore.load()?.let { ActiveWorkoutRoute(it.routineId) }
                AppShortcuts.Target.Log -> LogWorkoutRoute
                is AppShortcuts.Target.Routine -> RoutineRoute(target.id)
            }
        }
    }
}
