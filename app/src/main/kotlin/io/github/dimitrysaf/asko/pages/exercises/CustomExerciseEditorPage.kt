package io.github.dimitrysaf.asko.pages.exercises

import io.github.dimitrysaf.asko.components.WithTooltip
import io.github.dimitrysaf.asko.pages.routines.EditableTitle
import io.github.dimitrysaf.asko.pages.routines.RoutineNameDialog
import io.github.dimitrysaf.asko.data.model.ExerciseMedia
import io.github.dimitrysaf.asko.data.model.MediaType
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.remember
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import io.github.dimitrysaf.asko.components.MusclePreview
import io.github.dimitrysaf.asko.components.EquipmentPicture
import io.github.dimitrysaf.asko.components.MultiChoiceSheet
import io.github.dimitrysaf.asko.components.segmentedShapes
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.components.ExerciseDemo
import io.github.dimitrysaf.asko.components.pageContentPadding
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.model.Muscle
import io.github.dimitrysaf.asko.lib.di.appViewModel

/**
 * Making or changing an exercise of yours, as the routine editor: a red Discard top left (asking
 * first when something changed), its name as the title (tap to rename; a new one is named before
 * it opens), and Save top right (once it has a main muscle). Then its picture: your photos and
 * videos, then wger's muscles as you pick them, with Add in the corner; the muscles it mainly works,
 * the others it also works and its equipment (a list, each row opening a sheet to tick them in);
 * and how it's done.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CustomExerciseEditorPage(
    exerciseId: String,
    initialName: String,
    onBack: () -> Unit,
    onSaved: (exerciseId: String) -> Unit,
) {
    val viewModel = appViewModel {
        CustomExerciseEditorViewModel(exerciseId, initialName, it.customExerciseRepository, it.exerciseRepository, it.mediaRepository)
    }
    var confirmingDiscard by rememberSaveable { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    var choosing by rememberSaveable { mutableStateOf<Choosing?>(null) }
    var removingMedia by remember { mutableStateOf<ExerciseMedia?>(null) }
    val discard = { if (viewModel.dirty) confirmingDiscard = true else onBack() }
    // The system's photo picker: a photo, GIF or video of it, shown first wherever it is.
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris -> uris.forEach(viewModel::addMedia) }
    BackHandler(onBack = discard)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    Button(
                        onClick = discard,
                        modifier = Modifier.padding(start = Dimens.SpaceS),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) { Text(stringResource(R.string.action_discard)) }
                },
                // Its name, as the routine editor's: tap to rename.
                title = { EditableTitle(viewModel.name, stringResource(R.string.custom_rename)) { renaming = true } },
                actions = {
                    Button(
                        onClick = { viewModel.save(onSaved) },
                        enabled = viewModel.canSave,
                        modifier = Modifier.padding(end = Dimens.SpaceS),
                    ) { Text(stringResource(R.string.action_save)) }
                },
            )
        },
    ) { padding ->
        if (!viewModel.loaded) return@Scaffold
        LazyColumn(
            modifier = Modifier.consumeWindowInsets(padding).imePadding(),
            contentPadding = pageContentPadding(padding),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
        ) {
            // How it will look: your photos and videos first, then wger's muscles for the ones you
            // pick, then a page to add more; Remove one of yours in its top-right corner.
            item(key = "picture") {
                Box(Modifier.fillMaxWidth().height(280.dp)) {
                    ExerciseDemo(
                        exercise = viewModel.draft(),
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.fillMaxSize(),
                        onRemoveMedia = { removingMedia = it },
                        // The last page adds photos and videos, as many as you pick.
                        onAddMedia = { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                    )
                }
            }
            // What it works and needs, as one segmented list; each opens a sheet to tick them in.
            item(key = "choices") {
                Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                    ChoiceRow(0, stringResource(R.string.custom_main_muscles), viewModel.main.labels(), onClick = { choosing = Choosing.MAIN }) {
                        MusclePreview(viewModel.main, size = RowPreviewSize)
                    }
                    ChoiceRow(1, stringResource(R.string.custom_other_muscles), viewModel.other.labels(), onClick = { choosing = Choosing.OTHER }) {
                        MusclePreview(viewModel.other, size = RowPreviewSize, shade = 0.4f)
                    }
                    ChoiceRow(2, stringResource(R.string.discovery_equipment), viewModel.draft().equipment, onClick = { choosing = Choosing.EQUIPMENT }) {
                        EquipmentPicture(viewModel.draft().equipment.firstOrNull(), viewModel.library, size = RowPreviewSize)
                    }
                }
            }
            item(key = "how-to") {
                OutlinedTextField(
                    value = viewModel.instructions,
                    onValueChange = { viewModel.instructions = it },
                    label = { Text(stringResource(R.string.exercise_how_to)) },
                    minLines = 4,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    EditorDialogs(viewModel, renaming, { renaming = false }, removingMedia, { removingMedia = null })
    when (choosing) {
        Choosing.MAIN -> MultiChoiceSheet(
            title = stringResource(R.string.custom_main_muscles),
            options = Muscle.entries,
            selected = viewModel.main,
            label = { it.label },
            onToggle = viewModel::toggleMain,
            onDismiss = { choosing = null },
        ) { MusclePreview(setOf(it)) }
        // The others it works: any muscle that isn't a main one.
        Choosing.OTHER -> MultiChoiceSheet(
            title = stringResource(R.string.custom_other_muscles),
            options = Muscle.entries.filterNot { it in viewModel.main },
            selected = viewModel.other,
            label = { it.label },
            onToggle = viewModel::toggleOther,
            onDismiss = { choosing = null },
        ) { MusclePreview(setOf(it), shade = 0.4f) }
        Choosing.EQUIPMENT -> MultiChoiceSheet(
            title = stringResource(R.string.discovery_equipment),
            options = CustomExerciseEditorViewModel.EQUIPMENT,
            selected = viewModel.equipment,
            label = { it },
            onToggle = viewModel::toggleEquipment,
            onDismiss = { choosing = null },
        ) { EquipmentPicture(it, viewModel.library) }
        null -> {}
    }
    if (confirmingDiscard) {
        AlertDialog(
            onDismissRequest = { confirmingDiscard = false },
            title = { Text(stringResource(R.string.routine_discard_title)) },
            text = { Text(stringResource(R.string.custom_discard_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingDiscard = false
                        viewModel.discard(onBack)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.action_discard)) }
            },
            dismissButton = { TextButton(onClick = { confirmingDiscard = false }) { Text(stringResource(R.string.routine_keep_editing)) } },
        )
    }
}

/** Which sheet is open. */
private enum class Choosing { MAIN, OTHER, EQUIPMENT }

/** Renaming, and removing one of your photos or videos (asked first). */
@Composable
private fun EditorDialogs(
    viewModel: CustomExerciseEditorViewModel,
    renaming: Boolean,
    onRenamed: () -> Unit,
    removingMedia: ExerciseMedia?,
    onRemoveDone: () -> Unit,
) {
    if (renaming) {
        RoutineNameDialog(
            initialName = viewModel.name,
            title = stringResource(R.string.custom_name_title),
            placeholder = stringResource(R.string.custom_name_hint),
            confirmLabel = stringResource(R.string.action_save),
            onConfirm = {
                viewModel.name = it
                onRenamed()
            },
            onDismiss = onRenamed,
        )
    }
    removingMedia?.let { item ->
        AlertDialog(
            onDismissRequest = onRemoveDone,
            title = { Text(stringResource(if (item.type == MediaType.VIDEO) R.string.media_remove_video else R.string.media_remove_photo)) },
            text = { Text(stringResource(R.string.media_remove_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveDone()
                        viewModel.removeMedia(item)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.action_remove)) }
            },
            dismissButton = { TextButton(onClick = onRemoveDone) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** The muscles' names in the body's order, top to bottom. */
private fun Set<Muscle>.labels(): List<String> = Muscle.entries.filter { it in this }.map { it.label }

/**
 * One row of the choices list (place [index] of 3): its round [preview], [title], and what's
 * chosen ([chosen], "None" when nothing). Tap to choose.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChoiceRow(index: Int, title: String, chosen: List<String>, onClick: () -> Unit, preview: @Composable () -> Unit) {
    SegmentedListItem(
        verticalAlignment = Alignment.CenterVertically,
        onClick = onClick,
        shapes = segmentedShapes(index, 3),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        leadingContent = preview,
        supportingContent = { Text(chosen.joinToString().ifEmpty { stringResource(R.string.custom_none) }) },
        content = { Text(title) },
    )
}

private val RowPreviewSize = 48.dp
