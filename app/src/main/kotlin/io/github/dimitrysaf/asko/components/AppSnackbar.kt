package io.github.dimitrysaf.asko.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The app's one snackbar, shared by every page: each page's scaffold shows [state] above its own
 * bottom bar, so a message survives the page that sent it closing (a deleted workout's page
 * closes, its "Workout deleted · Undo" shows on the page underneath).
 */
class AppSnackbar(private val work: CoroutineScope) {
    val state = SnackbarHostState()
    private val main = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * Says [message] with an [undoLabel] button; tapping it runs [undo] (on [work], so it finishes
     * whatever page is open by then). A newer message replaces this one.
     */
    fun showUndo(message: String, undoLabel: String, undo: suspend () -> Unit) {
        main.launch {
            state.currentSnackbarData?.dismiss()
            val result = state.showSnackbar(message, actionLabel = undoLabel, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) work.launch { undo() }
        }
    }
}

/** The app's [AppSnackbar], provided at the top of the app. */
val LocalAppSnackbar = staticCompositionLocalOf<AppSnackbar> { error("No AppSnackbar provided") }
