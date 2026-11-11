package io.github.dimitrysaf.asko.pages.discovery

import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import java.text.NumberFormat
import io.github.dimitrysaf.asko.data.model.DownloadProgress
import io.github.dimitrysaf.asko.constants.Dimens
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.runtime.remember
import androidx.compose.material3.toShape
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.components.EmptyState
import io.github.dimitrysaf.asko.components.ExerciseBrowser
import io.github.dimitrysaf.asko.components.ExerciseBrowserSkeleton
import io.github.dimitrysaf.asko.lib.di.appViewModel

/** Browse and search the ExerciseDB exercise library. */
@Composable
fun DiscoveryPage(onExerciseClick: (exerciseId: String) -> Unit) {
    val viewModel = appViewModel { DiscoveryViewModel(it.exerciseRepository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val download by viewModel.download.collectAsStateWithLifecycle()
    // Once a download has shown, it stays until the list is there (no skeleton in between).
    var lastDownload by remember { mutableStateOf<DownloadProgress?>(null) }
    download?.let { lastDownload = it }

    Scaffold { padding ->
        // Keep the results above the keyboard while typing.
        val modifier = Modifier.padding(padding).consumeWindowInsets(padding).imePadding()
        // The results list takes the padding itself, so it scrolls right up to the bars.
        val listModifier = Modifier.consumeWindowInsets(padding).imePadding()
        when (val s = state) {
            // The first download shows its progress; reading the saved copy is quick, so a skeleton.
            DiscoveryState.Loading -> when (val progress = lastDownload) {
                null -> ExerciseBrowserSkeleton(listModifier.padding(top = padding.calculateTopPadding()))
                else -> LibraryDownload(progress, modifier)
            }
            DiscoveryState.Failed -> EmptyState(
                icon = painterResource(R.drawable.ic_download),
                message = stringResource(R.string.exercises_load_failed),
                modifier = modifier,
                actionLabel = stringResource(R.string.action_retry),
                onAction = viewModel::load,
            )
            is DiscoveryState.Loaded -> ExerciseBrowser(
                s.exercises,
                onExerciseClick = { onExerciseClick(it.id) },
                modifier = listModifier,
                contentPadding = padding,
            )
        }
    }
}

/**
 * The exercise library's first download: why it waits (only this once, then it's saved for the
 * gym), Material's wavy progress bar, how many exercises are in and about how long is left.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LibraryDownload(progress: DownloadProgress, modifier: Modifier = Modifier) {
    val shown by animateFloatAsState(progress.fraction, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "download")
    val number = remember { NumberFormat.getIntegerInstance() }
    Column(
        modifier = modifier.fillMaxSize().padding(Dimens.SpaceXl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(112.dp).background(MaterialTheme.colorScheme.primaryContainer, MaterialShapes.Cookie9Sided.toShape()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_download),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(Modifier.height(Dimens.SpaceL))
        Text(stringResource(R.string.download_title), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Dimens.SpaceS))
        Text(
            stringResource(R.string.download_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Dimens.SpaceXl))
        LinearWavyProgressIndicator(progress = { shown }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(Dimens.SpaceS))
        Row(Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.download_count, number.format(progress.exercises), number.format(progress.exerciseTotal)),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            Text(timeLeft(progress.remainingMs), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** "About 2 min left", "Less than a minute left", or "Working out the time…" at the start. */
@Composable
private fun timeLeft(remainingMs: Long?): String = when {
    remainingMs == null -> stringResource(R.string.download_estimating)
    remainingMs < 60_000 -> stringResource(R.string.download_under_minute)
    else -> {
        val minutes = ((remainingMs + 59_999) / 60_000).toInt()
        pluralStringResource(R.plurals.download_minutes_left, minutes, minutes)
    }
}
