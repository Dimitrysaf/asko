package io.github.dimitrysaf.asko.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Slider
import androidx.compose.runtime.mutableIntStateOf
import java.time.Instant
import kotlin.math.roundToInt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.offset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.asko.AskoApplication
import io.github.dimitrysaf.asko.R
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.data.model.Note
import io.github.dimitrysaf.asko.data.model.NoteSubject
import io.github.dimitrysaf.asko.data.model.NoteType
import io.github.dimitrysaf.asko.lib.format.formatDate
import kotlinx.coroutines.launch

/**
 * The notes of a routine, an exercise or a done workout: [NotesButton] with their count, opening
 * [NotesSheet]. Changes are saved as they're made.
 */
@Composable
fun SubjectNotes(subject: NoteSubject, id: String, modifier: Modifier = Modifier, tonal: Boolean = false) {
    val container = (LocalContext.current.applicationContext as AskoApplication).container
    val repository = container.noteRepository
    // An exercise's notes are shared by its versions in each library: it is the same exercise.
    val ids by produceState(listOf(id), subject, id) {
        if (subject == NoteSubject.EXERCISE) value = container.exerciseRepository.sameExerciseIds(id)
    }
    val notes by remember(subject, ids) { repository.notes(subject, ids) }.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    var open by rememberSaveable { mutableStateOf(false) }
    NotesButton(count = notes?.size ?: 0, onClick = { open = true }, modifier = modifier, tonal = tonal)
    if (open) {
        NotesSheet(
            notes = notes,
            onAdd = { note -> scope.launch { repository.add(subject, id, note) } },
            onEdit = { note -> scope.launch { repository.edit(note) } },
            onDelete = { note -> scope.launch { repository.delete(note.id) } },
            onDismiss = { open = false },
        )
    }
}

/**
 * The notes button: a standard icon button like the ones around it (plain in top bars, [tonal]
 * next to tonal ones, as in an exercise's header). How many notes there are is written in the
 * clipboard's blank middle (nothing when there are none), so it keeps the icon button's size.
 */
@Composable
fun NotesButton(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier, tonal: Boolean = false) {
    val description = if (count == 0) {
        stringResource(R.string.notes_title)
    } else {
        pluralStringResource(R.plurals.notes_count, count, count)
    }
    val content = @Composable {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_notes), contentDescription = description)
            if (count > 0) {
                Text(
                    if (count > 99) "99+" else count.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = NoteCountSize, lineHeight = NoteCountSize),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    // The clipboard's body sits below its clip.
                    modifier = Modifier.offset(y = 2.dp).clearAndSetSemantics {},
                )
            }
        }
    }
    if (tonal) {
        FilledTonalIconButton(onClick = onClick, modifier = modifier, content = content)
    } else {
        IconButton(onClick = onClick, modifier = modifier, content = content)
    }
}

private val NoteCountSize = 9.sp

/**
 * Every note, written out in full, as a segmented list (tap one to edit it); an empty state when
 * there are none, and Add note pinned at the bottom: pick a type, then write it.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotesSheet(
    notes: List<Note>?,
    onAdd: (Note) -> Unit,
    onEdit: (Note) -> Unit,
    onDelete: (Note) -> Unit,
    onDismiss: () -> Unit,
) {
    var choosingType by rememberSaveable { mutableStateOf(false) }
    var writing by rememberSaveable { mutableStateOf<NoteType?>(null) }
    var editing by remember { mutableStateOf<Note?>(null) }
    var deleting by remember { mutableStateOf<Note?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(start = Dimens.PageGutter, end = Dimens.PageGutter, bottom = Dimens.SpaceL)) {
            Text(
                stringResource(R.string.notes_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = Dimens.SpaceM),
            )
            when {
                notes == null -> Spacer(Modifier.height(Dimens.SpaceXl))
                notes.isEmpty() -> EmptyState(
                    painterResource(R.drawable.ic_notes),
                    stringResource(R.string.notes_empty),
                    compact = true,
                )
                // The list scrolls; Add note stays under it.
                else -> LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                ) {
                    itemsIndexed(notes, key = { _, note -> note.id }) { index, note ->
                        SegmentedListItem(
                            onClick = { editing = note },
                            shapes = segmentedShapes(index, notes.size),
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            leadingContent = { NoteBadge(note.type) },
                            // "Pain · Left shoulder · 6/10 · 11 Nov 2026": the type, its details, the day it was added.
                            overlineContent = { Text(noteSummary(note)) },
                            supportingContent = note.editedAt?.let { edited ->
                                { Text(stringResource(R.string.note_edited, edited.formatDate())) }
                            },
                            content = { Text(note.text) },
                        )
                    }
                }
            }
            Button(
                onClick = { choosingType = true },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth().padding(top = Dimens.SpaceL).height(56.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(Dimens.SpaceS))
                Text(stringResource(R.string.note_add))
            }
        }
    }

    if (choosingType) {
        NoteTypeSheet(
            onType = {
                choosingType = false
                writing = it
            },
            onDismiss = { choosingType = false },
        )
    }
    writing?.let { type ->
        NoteDialog(
            initial = Note(id = 0, type = type, text = "", createdAt = Instant.now()),
            onDone = {
                onAdd(it)
                writing = null
            },
            onDelete = null,
            onDismiss = { writing = null },
        )
    }
    editing?.let { note ->
        NoteDialog(
            initial = note,
            onDone = {
                // Saved as edited only when something changed.
                if (it != note) onEdit(it)
                editing = null
            },
            onDelete = {
                editing = null
                deleting = note
            },
            onDismiss = { editing = null },
        )
    }
    deleting?.let { note ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.note_delete_title)) },
            text = { Text(stringResource(R.string.note_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(note)
                        deleting = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** Every type of note with what it's for, as a segmented list, like the set types. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NoteTypeSheet(onType: (NoteType) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = Dimens.PageGutter, end = Dimens.PageGutter, bottom = Dimens.SpaceL),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            Text(
                stringResource(R.string.note_type_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = Dimens.SpaceM),
            )
            val types = NoteType.entries
            types.forEachIndexed { index, type ->
                SegmentedListItem(
                    onClick = { onType(type) },
                    shapes = segmentedShapes(index, types.size),
                    leadingContent = { NoteBadge(type) },
                    supportingContent = { Text(noteTypeDescription(type)) },
                    content = { Text(noteTypeName(type)) },
                )
            }
        }
    }
}

/**
 * Writing a note (a new one: [initial] has no text) or editing one; Done saves it. Below the note,
 * its type's details: Trainer's name, Gym (both suggesting what was typed before), or for Pain,
 * where and how bad. Editing also offers Delete note ([onDelete]), which asks first.
 */
@Composable
private fun NoteDialog(
    initial: Note,
    onDone: (Note) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val repository = (LocalContext.current.applicationContext as AskoApplication).container.noteRepository
    val type = initial.type
    var text by rememberSaveable { mutableStateOf(initial.text) }
    var trainer by rememberSaveable { mutableStateOf(initial.trainer.orEmpty()) }
    var gym by rememberSaveable { mutableStateOf(initial.gym.orEmpty()) }
    var bodyPart by rememberSaveable { mutableStateOf(initial.bodyPart.orEmpty()) }
    // 0: not rated.
    var painLevel by rememberSaveable { mutableIntStateOf(initial.painLevel ?: 0) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(noteTypeIcon(type), contentDescription = null) },
        title = { Text(stringResource(R.string.note_dialog_title, noteTypeName(type))) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.note_field)) },
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
                when (type) {
                    NoteType.TRAINER -> {
                        val names by repository.trainers.collectAsStateWithLifecycle(initialValue = emptyList())
                        SuggestingField(trainer, { trainer = it }, stringResource(R.string.note_trainer), names)
                    }
                    NoteType.SETUP -> {
                        val gyms by repository.gyms.collectAsStateWithLifecycle(initialValue = emptyList())
                        SuggestingField(gym, { gym = it }, stringResource(R.string.note_gym), gyms)
                    }
                    NoteType.PAIN -> {
                        OutlinedTextField(
                            value = bodyPart,
                            onValueChange = { bodyPart = it },
                            label = { Text(stringResource(R.string.note_body_part)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Column {
                            Text(
                                if (painLevel == 0) {
                                    stringResource(R.string.note_pain_not_rated)
                                } else {
                                    stringResource(R.string.note_pain_level, painLevel)
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            // 0 (all the way left) is "not rated", then 1 to 10.
                            Slider(
                                value = painLevel.toFloat(),
                                onValueChange = { painLevel = it.roundToInt() },
                                valueRange = 0f..10f,
                                steps = 9,
                            )
                        }
                    }
                    NoteType.SELF -> Unit
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDone(
                        initial.copy(
                            text = text.trim(),
                            trainer = trainer,
                            gym = gym,
                            bodyPart = bodyPart,
                            painLevel = painLevel.takeIf { it > 0 },
                        ).trimmedToType(),
                    )
                },
                enabled = text.isNotBlank(),
            ) { Text(stringResource(R.string.action_done)) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text(stringResource(R.string.note_delete)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}

/** A text field that offers what was typed before (Material's exposed dropdown menu, editable). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuggestingField(value: String, onValueChange: (String) -> Unit, label: String, suggestions: List<String>) {
    var expanded by remember { mutableStateOf(false) }
    val typed = value.trim()
    val matches = suggestions.filter { it.contains(typed, ignoreCase = true) && !it.equals(typed, ignoreCase = true) }
    val showing = expanded && matches.isNotEmpty()
    ExposedDropdownMenuBox(expanded = showing, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
        )
        ExposedDropdownMenu(expanded = showing, onDismissRequest = { expanded = false }) {
            matches.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion) },
                    onClick = {
                        onValueChange(suggestion)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** "Pain · Left shoulder · 6/10 · 11 Nov 2026": the type, the details it has, the day it was added. */
@Composable
private fun noteSummary(note: Note): String = listOfNotNull(
    noteTypeName(note.type),
    note.trainer,
    note.gym,
    note.bodyPart,
    note.painLevel?.let { stringResource(R.string.note_pain_short, it) },
    note.createdAt.formatDate(),
).joinToString(" · ")

/** A note type's mark: its icon in its shape and colour, as set types have theirs. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NoteBadge(type: NoteType, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val colors = MaterialTheme.colorScheme
    val (shape: Shape, container: Color, content: Color) = when (type) {
        NoteType.SELF -> Triple(CircleShape, colors.primaryContainer, colors.onPrimaryContainer)
        NoteType.TRAINER -> Triple(MaterialShapes.Cookie6Sided.toShape(), colors.tertiaryContainer, colors.onTertiaryContainer)
        NoteType.SETUP -> Triple(MaterialShapes.Cookie4Sided.toShape(), colors.secondaryContainer, colors.onSecondaryContainer)
        NoteType.PAIN -> Triple(MaterialShapes.SoftBurst.toShape(), colors.errorContainer, colors.onErrorContainer)
    }
    Box(modifier.size(size).background(container, shape), contentAlignment = Alignment.Center) {
        Icon(noteTypeIcon(type), contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun noteTypeIcon(type: NoteType): Painter = when (type) {
    NoteType.SELF -> rememberVectorPainter(Icons.Filled.Person)
    NoteType.TRAINER -> painterResource(R.drawable.ic_school)
    NoteType.SETUP -> rememberVectorPainter(Icons.Filled.Build)
    NoteType.PAIN -> rememberVectorPainter(Icons.Filled.Warning)
}

@Composable
fun noteTypeName(type: NoteType): String = stringResource(
    when (type) {
        NoteType.SELF -> R.string.note_type_self
        NoteType.TRAINER -> R.string.note_type_trainer
        NoteType.SETUP -> R.string.note_type_setup
        NoteType.PAIN -> R.string.note_type_pain
    },
)

@Composable
private fun noteTypeDescription(type: NoteType): String = stringResource(
    when (type) {
        NoteType.SELF -> R.string.note_type_self_hint
        NoteType.TRAINER -> R.string.note_type_trainer_hint
        NoteType.SETUP -> R.string.note_type_setup_hint
        NoteType.PAIN -> R.string.note_type_pain_hint
    },
)
