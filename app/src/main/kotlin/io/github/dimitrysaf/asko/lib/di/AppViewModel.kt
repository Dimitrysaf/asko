package io.github.dimitrysaf.asko.lib.di

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.dimitrysaf.asko.AskoApplication

/**
 * Creates a ViewModel with access to [AppContainer]:
 * `val vm = appViewModel { RoutinesViewModel(it.workoutRepository) }`
 */
@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: (AppContainer) -> VM): VM {
    val container = (LocalContext.current.applicationContext as AskoApplication).container
    return viewModel { create(container) }
}
