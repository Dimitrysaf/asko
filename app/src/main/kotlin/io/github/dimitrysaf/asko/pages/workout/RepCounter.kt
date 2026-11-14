package io.github.dimitrysaf.asko.pages.workout

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset

import android.Manifest
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.dimitrysaf.asko.R
import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import io.github.dimitrysaf.asko.constants.Dimens
import io.github.dimitrysaf.asko.AskoApplication
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dimitrysaf.asko.data.settings.SettingsRepository
import io.github.dimitrysaf.asko.lib.reps.Joint
import io.github.dimitrysaf.asko.lib.reps.RepDetector
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.Executor
import kotlin.math.abs
import kotlin.math.atan2

/**
 * The metronome rep counter, in the demo's place while it counts (its toggle is in the exercise's
 * header). [count] is the reps so far, [onCount] a new count, [onTarget] the set's [target]
 * reached (none for a set to failure: it counts until stopped). The camera counter is
 * [CameraReps], behind the whole screen.
 */
@Composable
internal fun RepCounter(
    exerciseId: String,
    settings: SettingsRepository,
    count: Int,
    target: Int?,
    onCount: (Int) -> Unit,
    onTarget: () -> Unit,
) {
    val speak = rememberSpeaker()
    Box(
        Modifier
            .widthIn(max = MediaMaxWidth)
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center,
    ) {
        MetronomeCounter(exerciseId, settings, count, target, speak, onCount, onTarget)
    }
}

/**
 * Counts aloud at the exercise's pace ("one… two…"), with the count in the middle of a ring that
 * fills towards the target, as the rest clock does; − / + change the pace (kept per exercise).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MetronomeCounter(
    exerciseId: String,
    settings: SettingsRepository,
    count: Int,
    target: Int?,
    speak: (String) -> Unit,
    onCount: (Int) -> Unit,
    onTarget: () -> Unit,
) {
    var paceMs by remember(exerciseId) { mutableIntStateOf(settings.repPaceMs(exerciseId)) }
    val pace by rememberUpdatedState(paceMs)
    val counted by rememberUpdatedState(count)
    LaunchedEffect(Unit) {
        while (true) {
            delay(pace.toLong())
            val next = counted + 1
            onCount(next)
            speak(next.toString())
            if (target != null && next >= target) {
                onTarget()
                break
            }
        }
    }
    val stroke = with(LocalDensity.current) { Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round) }
    val ring by animateFloatAsState(if (target == null || target == 0) 0f else count.toFloat() / target, label = "reps ring")
    Box(Modifier.fillMaxSize().padding(Dimens.SpaceL), contentAlignment = Alignment.Center) {
        CircularWavyProgressIndicator(progress = { ring }, modifier = Modifier.fillMaxSize(), stroke = stroke, trackStroke = stroke, wavelength = 40.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
            Text(count.toString(), style = MaterialTheme.typography.displayLarge)
            Text(
                stringResource(R.string.rep_counter_pace, formatPace(paceMs)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS), verticalAlignment = Alignment.CenterVertically) {
                PaceButton(painterResource(R.drawable.ic_remove), stringResource(R.string.rep_counter_shorter)) {
                    paceMs = (paceMs - PACE_STEP_MS).coerceAtLeast(MIN_PACE_MS)
                    settings.setRepPaceMs(exerciseId, paceMs)
                }
                PaceButton(rememberVectorPainter(Icons.Filled.Add), stringResource(R.string.rep_counter_longer)) {
                    paceMs = (paceMs + PACE_STEP_MS).coerceAtMost(MAX_PACE_MS)
                    settings.setRepPaceMs(exerciseId, paceMs)
                }
            }
        }
    }
}

/** − 0.5 s / + 0.5 s, as the rest clock's − 15 s / + 15 s. */
@Composable
private fun PaceButton(icon: Painter, description: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        contentPadding = ButtonDefaults.SmallContentPadding,
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(Dimens.SpaceXs))
        Text(stringResource(R.string.rep_counter_step))
    }
}

/** "2 s", "2.5 s". */
private fun formatPace(ms: Int): String =
    if (ms % 1000 == 0) (ms / 1000).toString() else String.format(Locale.getDefault(), "%.1f", ms / 1000.0)

private const val PACE_STEP_MS = 500
private const val MIN_PACE_MS = 1_000
private const val MAX_PACE_MS = 6_000

/**
 * The camera rep counter: the front camera's picture filling [modifier] under a [wash] of colour,
 * with Google's MediaPipe (on the phone) following your body with its heavy pose model: the joint
 * [joint] (from the exercise's muscles; found from the movement when null) among the elbows,
 * knees and hips, and your body drawn over it as a stick figure, the joint being counted in the
 * app's colour. With no body in view, its hand model looks for a hand: for an arm exercise (or
 * one with no joint) a hand shown on its own is enough, and its rise and fall is counted.
 *
 * Each full movement is said aloud and [onRep] gets the reps so far, while [counting] (a reps set
 * to do). [onInView]: someone (or, where it counts, a hand) is in view; [onTooDark]: the picture
 * has been too dark to see anyone for a moment. A new [setKey] starts the count over; the camera
 * itself stays on.
 */
@Composable
internal fun CameraReps(
    setKey: Any,
    joint: Joint?,
    counting: Boolean,
    onRep: (Int) -> Unit,
    onInView: (Boolean) -> Unit,
    onTooDark: (Boolean) -> Unit,
    wash: Color,
    modifier: Modifier = Modifier,
) {
    val inView by rememberUpdatedState(onInView)
    val tooDark by rememberUpdatedState(onTooDark)
    // Off a reps set (a rest, a timed set…), the camera stays on but counts and says nothing.
    val countingNow by rememberUpdatedState(counting)
    // A hand alone stands in for the body only where the arms do the work.
    val handsCount by rememberUpdatedState(joint == null || joint == Joint.ELBOW)
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val speak = rememberSpeaker()
    val detector = remember(setKey, joint) { RepDetector(joint) }
    val current by rememberUpdatedState(detector)
    val rep by rememberUpdatedState { reps: Int ->
        speak(reps.toString())
        onRep(reps)
    }
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    var body by remember { mutableStateOf<BodyFrame?>(null) }
    DisposableEffect(lifecycle) {
        // Everything MediaPipe does happens on this one thread, a frame at a time: the camera
        // drops frames while one is being looked at (keep only the latest).
        val analysisThread = Executors.newSingleThreadExecutor()
        val main = ContextCompat.getMainExecutor(context)
        val stopped = AtomicBoolean(false)
        var trackers: Trackers? = null
        // Loading the models takes a moment: done on that thread, not the screen's.
        analysisThread.execute { if (!stopped.get()) trackers = Trackers.create(context) }
        var darkSince: Long? = null
        var analyzing: ImageAnalysis? = null
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            if (stopped.get()) return@addListener
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
            analyzing = analysis
            analysis.setAnalyzer(analysisThread) { frame ->
                val tracking = trackers
                if (stopped.get() || tracking == null) {
                    frame.close()
                    return@setAnalyzer
                }
                val time = frame.imageInfo.timestamp / 1_000_000
                val rotation = frame.imageInfo.rotationDegrees
                // The picture upright, as MediaPipe places the points in it.
                val (width, height) = if (rotation % 180 == 0) frame.width to frame.height else frame.height to frame.width
                val bitmap = frame.toBitmap()
                frame.close()
                // Too dark to see anyone, for a moment: say so (a flicker doesn't count).
                val dark = brightness(bitmap) < DARK_LUMA
                darkSince = if (dark) darkSince ?: time else null
                val isTooDark = darkSince?.let { time - it >= DARK_MS } ?: false
                val image = BitmapImageBuilder(bitmap).build()
                val options = ImageProcessingOptions.builder().setRotationDegrees(rotation).build()
                val pose = runCatching { tracking.pose.detectForVideo(image, options, time).landmarks().firstOrNull() }.getOrNull()
                // Only a real body counts or is drawn: anything less (a hand up close) isn't one.
                val person = pose?.takeIf(::isBody)
                val hand = if (person == null && handsCount) {
                    runCatching { tracking.hand.detectForVideo(image, options, time).landmarks().firstOrNull() }.getOrNull()
                } else {
                    null
                }
                val there = person != null || hand != null
                val reps = current
                val angles = when {
                    person != null -> anglesOf(person, width, height)
                    hand != null -> mapOf(Joint.HAND to (1 - hand[WRIST].y()) * 180.0)
                    else -> emptyMap()
                }
                val counted = there && countingNow && reps.onFrame(angles, time)
                val total = reps.reps
                val seen = person.orEmpty().withIndex()
                    .filter { (_, point) -> point.visibility().orElse(0f) > SEEN }
                    .associate { (i, point) -> i to Offset(point.x() * width, point.y() * height) }
                val handPoints = hand.orEmpty().withIndex().associate { (i, point) -> i to Offset(point.x() * width, point.y() * height) }
                val drawn = BodyFrame(seen, handPoints, width, height, if (person != null) reps.following else Joint.HAND)
                main.execute {
                    if (stopped.get()) return@execute
                    body = drawn
                    inView(there)
                    tooDark(isTooDark)
                    if (counted && current === reps) rep(total)
                }
            }
            provider.unbindAll()
            provider.bindToLifecycle(lifecycle, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)
        }, main)
        onDispose {
            stopped.set(true)
            analyzing?.clearAnalyzer()
            if (providerFuture.isDone) providerFuture.get().unbindAll()
            // After any frame still being looked at, on the same thread.
            analysisThread.execute { trackers?.close() }
            analysisThread.shutdown()
        }
    }
    Box(modifier) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(wash))
        StickFigure(body, Modifier.fillMaxSize())
    }
}

/**
 * MediaPipe's two trackers, loaded from the app's assets: the heavy body model and the hand model,
 * on the phone's GPU (on the CPU where the GPU can't run them). Each takes the frames in turn.
 */
private class Trackers(val pose: PoseLandmarker, val hand: HandLandmarker) {
    fun close() {
        pose.close()
        hand.close()
    }

    companion object {
        fun create(context: Context): Trackers? = runCatching { create(context, Delegate.GPU) }
            .recoverCatching { create(context, Delegate.CPU) }
            .getOrNull()

        private fun create(context: Context, delegate: Delegate): Trackers {
            fun base(model: String) = BaseOptions.builder().setModelAssetPath(model).setDelegate(delegate).build()
            val pose = PoseLandmarker.createFromOptions(
                context,
                PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(base(POSE_MODEL))
                    .setRunningMode(RunningMode.VIDEO)
                    .setNumPoses(1)
                    .build(),
            )
            val hand = try {
                HandLandmarker.createFromOptions(
                    context,
                    HandLandmarker.HandLandmarkerOptions.builder()
                        .setBaseOptions(base(HAND_MODEL))
                        .setRunningMode(RunningMode.VIDEO)
                        .setNumHands(1)
                        .build(),
                )
            } catch (e: RuntimeException) {
                pose.close()
                throw e
            }
            return Trackers(pose, hand)
        }
    }
}

private const val POSE_MODEL = "pose_landmarker_heavy.task"
private const val HAND_MODEL = "hand_landmarker.task"

/** How bright the picture is, 0–255: the average of a grid of its pixels' luma. */
private fun brightness(bitmap: Bitmap): Double {
    val stepX = (bitmap.width / BRIGHTNESS_GRID).coerceAtLeast(1)
    val stepY = (bitmap.height / BRIGHTNESS_GRID).coerceAtLeast(1)
    var total = 0.0
    var count = 0
    for (y in 0 until bitmap.height step stepY) {
        for (x in 0 until bitmap.width step stepX) {
            val c = bitmap.getPixel(x, y)
            total += 0.299 * android.graphics.Color.red(c) + 0.587 * android.graphics.Color.green(c) + 0.114 * android.graphics.Color.blue(c)
            count++
        }
    }
    return if (count == 0) 255.0 else total / count
}

private const val BRIGHTNESS_GRID = 24

/** Below this average brightness, the picture is too dark to see anyone in. */
private const val DARK_LUMA = 40.0

/** How long it must stay that dark before saying so. */
private const val DARK_MS = 1_000L

/**
 * One frame's body: the points seen well enough (in the upright picture, [width] × [height]), a
 * hand seen on its own ([hand]: MediaPipe's 21 hand points), and the joint counted.
 */
private class BodyFrame(val points: Map<Int, Offset>, val hand: Map<Int, Offset>, val width: Int, val height: Int, val following: Joint?)

/** How sure MediaPipe must be that a point is in view for it to be used (counted or drawn). */
private const val SEEN = 0.6f

/**
 * A person, not a guess: the head (nose), a shoulder and a hip all clearly seen. One side is
 * enough, so a side-on push-up (the far side hidden) still counts.
 */
private fun isBody(pose: List<NormalizedLandmark>): Boolean {
    fun seen(point: Int) = (pose.getOrNull(point)?.visibility()?.orElse(0f) ?: 0f) > SEEN
    return seen(P.NOSE) && (seen(P.LEFT_SHOULDER) || seen(P.RIGHT_SHOULDER)) && (seen(P.LEFT_HIP) || seen(P.RIGHT_HIP))
}

/**
 * Your body as lines over the camera's picture, where the picture shows it: scaled and cropped
 * as the preview is, mirrored like the front camera. Head, neck, arms and hands, torso, legs and
 * feet; points not seen well are left out, and the limbs of the joint being counted are in the
 * app's colour. A hand seen on its own is drawn in the app's colour too.
 */
@Composable
private fun StickFigure(frame: BodyFrame?, modifier: Modifier = Modifier) {
    val bone = MaterialTheme.colorScheme.onBackground
    val counted = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val f = frame ?: return@Canvas
        if (f.width == 0 || f.height == 0) return@Canvas
        val scale = maxOf(size.width / f.width, size.height / f.height)
        val dx = (size.width - f.width * scale) / 2
        val dy = (size.height - f.height * scale) / 2
        fun place(point: Offset) = Offset(size.width - (dx + point.x * scale), dy + point.y * scale)
        fun at(point: Int): Offset? = f.points[point]?.let(::place)
        val thin = 4.dp.toPx()
        val thick = 7.dp.toPx()
        val hot = f.following?.let(COUNTED_BONES::get).orEmpty()
        // The head: a ring around the nose, as wide as the ears are apart; the neck down to between the shoulders.
        at(P.NOSE)?.let { nose ->
            val left = at(P.LEFT_EAR)
            val right = at(P.RIGHT_EAR)
            val radius = if (left != null && right != null) (left - right).getDistance() * 0.65f else 20.dp.toPx()
            drawCircle(bone, radius, nose, style = Stroke(thin))
            val l = at(P.LEFT_SHOULDER)
            val r = at(P.RIGHT_SHOULDER)
            if (l != null && r != null) drawLine(bone, Offset(nose.x, nose.y + radius), (l + r) / 2f, thin, StrokeCap.Round)
        }
        for ((a, b) in BONES) {
            val from = at(a) ?: continue
            val to = at(b) ?: continue
            val isCounted = (a to b) in hot
            drawLine(if (isCounted) counted else bone, from, to, if (isCounted) thick else thin, StrokeCap.Round)
        }
        for (point in JOINTS) at(point)?.let { drawCircle(bone, thin * 1.2f, it) }
        // A hand on its own: its bones and knuckles.
        for ((a, b) in HAND_BONES) {
            val from = f.hand[a]?.let(::place) ?: continue
            val to = f.hand[b]?.let(::place) ?: continue
            drawLine(counted, from, to, thin, StrokeCap.Round)
        }
        f.hand.values.forEach { drawCircle(counted, thin * 0.9f, place(it)) }
    }
}

/** MediaPipe's body points, by their numbers (the same as ML Kit's). */
private object P {
    const val NOSE = 0
    const val LEFT_EAR = 7
    const val RIGHT_EAR = 8
    const val LEFT_SHOULDER = 11
    const val RIGHT_SHOULDER = 12
    const val LEFT_ELBOW = 13
    const val RIGHT_ELBOW = 14
    const val LEFT_WRIST = 15
    const val RIGHT_WRIST = 16
    const val LEFT_PINKY = 17
    const val RIGHT_PINKY = 18
    const val LEFT_INDEX = 19
    const val RIGHT_INDEX = 20
    const val LEFT_THUMB = 21
    const val RIGHT_THUMB = 22
    const val LEFT_HIP = 23
    const val RIGHT_HIP = 24
    const val LEFT_KNEE = 25
    const val RIGHT_KNEE = 26
    const val LEFT_ANKLE = 27
    const val RIGHT_ANKLE = 28
    const val LEFT_HEEL = 29
    const val RIGHT_HEEL = 30
    const val LEFT_FOOT_INDEX = 31
    const val RIGHT_FOOT_INDEX = 32
}

/** The wrist among MediaPipe's 21 hand points. */
private const val WRIST = 0

/** The lines between a hand's points: the palm and each finger. */
private val HAND_BONES: List<Pair<Int, Int>> = listOf(
    0 to 1, 1 to 2, 2 to 3, 3 to 4,
    0 to 5, 5 to 6, 6 to 7, 7 to 8,
    5 to 9, 9 to 10, 10 to 11, 11 to 12,
    9 to 13, 13 to 14, 14 to 15, 15 to 16,
    13 to 17, 0 to 17, 17 to 18, 18 to 19, 19 to 20,
)

/** The lines between the body's points: torso, arms and hands, legs and feet. */
private val BONES: List<Pair<Int, Int>> = listOf(
    P.LEFT_SHOULDER to P.RIGHT_SHOULDER, P.LEFT_SHOULDER to P.LEFT_HIP, P.RIGHT_SHOULDER to P.RIGHT_HIP, P.LEFT_HIP to P.RIGHT_HIP,
    P.LEFT_SHOULDER to P.LEFT_ELBOW, P.LEFT_ELBOW to P.LEFT_WRIST, P.RIGHT_SHOULDER to P.RIGHT_ELBOW, P.RIGHT_ELBOW to P.RIGHT_WRIST,
    P.LEFT_WRIST to P.LEFT_INDEX, P.LEFT_WRIST to P.LEFT_PINKY, P.LEFT_WRIST to P.LEFT_THUMB, P.LEFT_INDEX to P.LEFT_PINKY,
    P.RIGHT_WRIST to P.RIGHT_INDEX, P.RIGHT_WRIST to P.RIGHT_PINKY, P.RIGHT_WRIST to P.RIGHT_THUMB, P.RIGHT_INDEX to P.RIGHT_PINKY,
    P.LEFT_HIP to P.LEFT_KNEE, P.LEFT_KNEE to P.LEFT_ANKLE, P.RIGHT_HIP to P.RIGHT_KNEE, P.RIGHT_KNEE to P.RIGHT_ANKLE,
    P.LEFT_ANKLE to P.LEFT_HEEL, P.LEFT_HEEL to P.LEFT_FOOT_INDEX, P.LEFT_ANKLE to P.LEFT_FOOT_INDEX,
    P.RIGHT_ANKLE to P.RIGHT_HEEL, P.RIGHT_HEEL to P.RIGHT_FOOT_INDEX, P.RIGHT_ANKLE to P.RIGHT_FOOT_INDEX,
)

/** The lines either side of each joint the counter can follow. */
private val COUNTED_BONES: Map<Joint, Set<Pair<Int, Int>>> = mapOf(
    Joint.ELBOW to setOf(P.LEFT_SHOULDER to P.LEFT_ELBOW, P.LEFT_ELBOW to P.LEFT_WRIST, P.RIGHT_SHOULDER to P.RIGHT_ELBOW, P.RIGHT_ELBOW to P.RIGHT_WRIST),
    Joint.KNEE to setOf(P.LEFT_HIP to P.LEFT_KNEE, P.LEFT_KNEE to P.LEFT_ANKLE, P.RIGHT_HIP to P.RIGHT_KNEE, P.RIGHT_KNEE to P.RIGHT_ANKLE),
    Joint.HIP to setOf(P.LEFT_SHOULDER to P.LEFT_HIP, P.LEFT_HIP to P.LEFT_KNEE, P.RIGHT_SHOULDER to P.RIGHT_HIP, P.RIGHT_HIP to P.RIGHT_KNEE),
)

/** The points marked with a dot: shoulders, elbows, wrists, hips, knees, ankles. */
private val JOINTS: List<Int> = listOf(
    P.LEFT_SHOULDER, P.RIGHT_SHOULDER, P.LEFT_ELBOW, P.RIGHT_ELBOW, P.LEFT_WRIST, P.RIGHT_WRIST,
    P.LEFT_HIP, P.RIGHT_HIP, P.LEFT_KNEE, P.RIGHT_KNEE, P.LEFT_ANKLE, P.RIGHT_ANKLE,
)

/**
 * The body's elbow, knee and hip angles, each the average of the sides seen well (none when
 * neither is). MediaPipe's points are 0–1 across and down; they're measured in the upright
 * picture's pixels ([width] × [height]) so a wide or tall picture doesn't bend the angles.
 */
private fun anglesOf(pose: List<NormalizedLandmark>, width: Int, height: Int): Map<Joint, Double?> {
    fun sides(la: Int, lb: Int, lc: Int, ra: Int, rb: Int, rc: Int): Double? {
        val angles = listOfNotNull(angleAt(pose, la, lb, lc, width, height), angleAt(pose, ra, rb, rc, width, height))
        return if (angles.isEmpty()) null else angles.average()
    }
    return mapOf(
        Joint.ELBOW to sides(P.LEFT_SHOULDER, P.LEFT_ELBOW, P.LEFT_WRIST, P.RIGHT_SHOULDER, P.RIGHT_ELBOW, P.RIGHT_WRIST),
        Joint.KNEE to sides(P.LEFT_HIP, P.LEFT_KNEE, P.LEFT_ANKLE, P.RIGHT_HIP, P.RIGHT_KNEE, P.RIGHT_ANKLE),
        Joint.HIP to sides(P.LEFT_SHOULDER, P.LEFT_HIP, P.LEFT_KNEE, P.RIGHT_SHOULDER, P.RIGHT_HIP, P.RIGHT_KNEE),
    )
}

/** The angle at [b] between [a] and [c], 0–180°, when all three are seen well enough. */
private fun angleAt(pose: List<NormalizedLandmark>, a: Int, b: Int, c: Int, width: Int, height: Int): Double? {
    val points = listOf(a, b, c).map { i ->
        val point = pose.getOrNull(i)?.takeIf { it.visibility().orElse(0f) > SEEN } ?: return null
        Offset(point.x() * width, point.y() * height)
    }
    return angle(points[0], points[1], points[2])
}

private fun angle(a: Offset, b: Offset, c: Offset): Double {
    var degrees = abs(Math.toDegrees(atan2((a.y - b.y).toDouble(), (a.x - b.x).toDouble()) - atan2((c.y - b.y).toDouble(), (c.x - b.x).toDouble())))
    if (degrees > 180) degrees = 360 - degrees
    return degrees
}

/** Says a word with the phone's own text-to-speech (offline); quiet until it's ready, and with Say reps aloud off. */
@Composable
internal fun rememberSpeaker(): (String) -> Unit {
    val context = LocalContext.current
    // Settings › Say reps aloud: off, the counter counts silently.
    val settings = (context.applicationContext as AskoApplication).container.settings
    val aloud by settings.sayReps.collectAsStateWithLifecycle()
    var ready by remember { mutableStateOf(false) }
    val tts = remember { TextToSpeech(context) { status -> ready = status == TextToSpeech.SUCCESS } }
    DisposableEffect(tts) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }
    return { text -> if (ready && aloud) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text) }
}
