package io.github.dimitrysaf.asko.pages.workout

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.PointF
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn as AndroidOptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import io.github.dimitrysaf.asko.R
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
 * with ML Kit's pose detection (on the phone) following the elbows, knees and hips, and your body
 * drawn over it as a stick figure, the joint being counted in the app's colour. Each full movement
 * is said aloud and [onRep] gets the reps so far. A new [setKey] starts the count over (the camera
 * stays on).
 */
@AndroidOptIn(ExperimentalGetImage::class)
@Composable
internal fun CameraReps(setKey: Any, onRep: (Int) -> Unit, onInView: (Boolean) -> Unit, wash: Color, modifier: Modifier = Modifier) {
    val inView by rememberUpdatedState(onInView)
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val speak = rememberSpeaker()
    val detector = remember(setKey) { RepDetector() }
    val current by rememberUpdatedState(detector)
    val rep by rememberUpdatedState { reps: Int ->
        speak(reps.toString())
        onRep(reps)
    }
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    var body by remember { mutableStateOf<BodyFrame?>(null) }
    DisposableEffect(lifecycle) {
        val poses = PoseDetection.getClient(PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.STREAM_MODE).build())
        val analysisThread = Executors.newSingleThreadExecutor()
        val main = ContextCompat.getMainExecutor(context)
        // Stopped (left the set, a rest began…): a frame ML Kit is still on finishes on its own
        // thread and is ignored. Its results never go to analysisThread, which is shut down then.
        val stopped = AtomicBoolean(false)
        val direct = Executor { it.run() }
        var analyzing: ImageAnalysis? = null
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            if (stopped.get()) return@addListener
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
            val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            analyzing = analysis
            analysis.setAnalyzer(analysisThread) { frame ->
                val image = frame.image
                if (image == null) {
                    frame.close()
                    return@setAnalyzer
                }
                val time = frame.imageInfo.timestamp / 1_000_000
                val rotation = frame.imageInfo.rotationDegrees
                // The picture upright, as ML Kit places the points in it.
                val (width, height) = if (rotation % 180 == 0) frame.width to frame.height else frame.height to frame.width
                poses.process(InputImage.fromMediaImage(image, rotation))
                    .addOnSuccessListener(direct) { pose ->
                        if (stopped.get()) return@addOnSuccessListener
                        val reps = current
                        // Only a real body counts or is drawn: ML Kit guesses a whole skeleton
                        // from little (a hand up close), so anything less is nobody there.
                        val there = isBody(pose)
                        val counted = there && reps.onFrame(anglesOf(pose), time)
                        val total = reps.reps
                        val seen = if (!there) emptyMap() else pose.allPoseLandmarks
                            .filter { it.inFrameLikelihood > SEEN }
                            .associate { it.landmarkType to Offset(it.position.x, it.position.y) }
                        val drawn = BodyFrame(seen, width, height, reps.following)
                        main.execute {
                            if (stopped.get()) return@execute
                            body = drawn
                            inView(there)
                            if (counted && current === reps) rep(total)
                        }
                    }
                    // Every frame is let go, finished or cancelled, so the camera can send the next.
                    .addOnCompleteListener(direct) { frame.close() }
            }
            provider.unbindAll()
            provider.bindToLifecycle(lifecycle, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)
        }, main)
        onDispose {
            stopped.set(true)
            analyzing?.clearAnalyzer()
            if (providerFuture.isDone) providerFuture.get().unbindAll()
            poses.close()
            analysisThread.shutdown()
        }
    }
    Box(modifier) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(wash))
        StickFigure(body, Modifier.fillMaxSize())
    }
}

/** One frame's body: the points seen well enough (in the upright picture, [width] × [height]), and the joint counted. */
private class BodyFrame(val points: Map<Int, Offset>, val width: Int, val height: Int, val following: Joint?)

/** How sure ML Kit must be that a point is in view for it to be used (counted or drawn). */
private const val SEEN = 0.8f

/**
 * A person, not a guess: the head (nose), a shoulder and a hip all clearly seen. One side is
 * enough, so a side-on push-up (the far side hidden) still counts.
 */
private fun isBody(pose: Pose): Boolean {
    fun seen(point: Int) = (pose.getPoseLandmark(point)?.inFrameLikelihood ?: 0f) > SEEN
    return seen(PoseLandmark.NOSE) &&
        (seen(PoseLandmark.LEFT_SHOULDER) || seen(PoseLandmark.RIGHT_SHOULDER)) &&
        (seen(PoseLandmark.LEFT_HIP) || seen(PoseLandmark.RIGHT_HIP))
}

/**
 * Your body as lines over the camera's picture, where the picture shows it: scaled and cropped
 * as the preview is, mirrored like the front camera. Head, neck, arms and hands, torso, legs and
 * feet; points not seen well are left out, and the limbs of the joint being counted are in the
 * app's colour.
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
        fun at(point: Int): Offset? = f.points[point]?.let { Offset(size.width - (dx + it.x * scale), dy + it.y * scale) }
        val thin = 4.dp.toPx()
        val thick = 7.dp.toPx()
        val hot = f.following?.let(COUNTED_BONES::get).orEmpty()
        // The head: a ring around the nose, as wide as the ears are apart; the neck down to between the shoulders.
        at(PoseLandmark.NOSE)?.let { nose ->
            val left = at(PoseLandmark.LEFT_EAR)
            val right = at(PoseLandmark.RIGHT_EAR)
            val radius = if (left != null && right != null) (left - right).getDistance() * 0.65f else 20.dp.toPx()
            drawCircle(bone, radius, nose, style = Stroke(thin))
            val l = at(PoseLandmark.LEFT_SHOULDER)
            val r = at(PoseLandmark.RIGHT_SHOULDER)
            if (l != null && r != null) drawLine(bone, Offset(nose.x, nose.y + radius), (l + r) / 2f, thin, StrokeCap.Round)
        }
        for ((a, b) in BONES) {
            val from = at(a) ?: continue
            val to = at(b) ?: continue
            val isCounted = (a to b) in hot
            drawLine(if (isCounted) counted else bone, from, to, if (isCounted) thick else thin, StrokeCap.Round)
        }
        for (point in JOINTS) at(point)?.let { drawCircle(bone, thin * 1.2f, it) }
    }
}

/** The lines between ML Kit's points: torso, arms and hands, legs and feet. */
private val BONES: List<Pair<Int, Int>> = run {
    listOf(
        PoseLandmark.LEFT_SHOULDER to PoseLandmark.RIGHT_SHOULDER, PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP, PoseLandmark.LEFT_HIP to PoseLandmark.RIGHT_HIP,
        PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
        PoseLandmark.LEFT_WRIST to PoseLandmark.LEFT_INDEX, PoseLandmark.LEFT_WRIST to PoseLandmark.LEFT_PINKY, PoseLandmark.LEFT_WRIST to PoseLandmark.LEFT_THUMB, PoseLandmark.LEFT_INDEX to PoseLandmark.LEFT_PINKY,
        PoseLandmark.RIGHT_WRIST to PoseLandmark.RIGHT_INDEX, PoseLandmark.RIGHT_WRIST to PoseLandmark.RIGHT_PINKY, PoseLandmark.RIGHT_WRIST to PoseLandmark.RIGHT_THUMB, PoseLandmark.RIGHT_INDEX to PoseLandmark.RIGHT_PINKY,
        PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE,
        PoseLandmark.LEFT_ANKLE to PoseLandmark.LEFT_HEEL, PoseLandmark.LEFT_HEEL to PoseLandmark.LEFT_FOOT_INDEX, PoseLandmark.LEFT_ANKLE to PoseLandmark.LEFT_FOOT_INDEX,
        PoseLandmark.RIGHT_ANKLE to PoseLandmark.RIGHT_HEEL, PoseLandmark.RIGHT_HEEL to PoseLandmark.RIGHT_FOOT_INDEX, PoseLandmark.RIGHT_ANKLE to PoseLandmark.RIGHT_FOOT_INDEX,
    )
}

/** The lines either side of each joint the counter can follow. */
private val COUNTED_BONES: Map<Joint, Set<Pair<Int, Int>>> = run {
    mapOf(
        Joint.ELBOW to setOf(PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST),
        Joint.KNEE to setOf(PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE),
        Joint.HIP to setOf(PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE),
    )
}

/** The points marked with a dot: shoulders, elbows, wrists, hips, knees, ankles. */
private val JOINTS: List<Int> = run {
    listOf(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW, PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST, PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE, PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_ANKLE)
}

/** The pose's elbow, knee and hip angles, each the average of the sides seen well (none when neither is). */
private fun anglesOf(pose: Pose): Map<Joint, Double?> = mapOf(
    Joint.ELBOW to sides(pose, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST),
    Joint.KNEE to sides(pose, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE),
    Joint.HIP to sides(pose, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE),
)

private fun sides(pose: Pose, la: Int, lb: Int, lc: Int, ra: Int, rb: Int, rc: Int): Double? {
    val angles = listOfNotNull(angleAt(pose, la, lb, lc), angleAt(pose, ra, rb, rc))
    return if (angles.isEmpty()) null else angles.average()
}

/** The angle at [b] between [a] and [c], 0–180°, when all three are seen well enough. */
private fun angleAt(pose: Pose, a: Int, b: Int, c: Int): Double? {
    val points = listOf(a, b, c).map { pose.getPoseLandmark(it)?.takeIf { l -> l.inFrameLikelihood > SEEN }?.position ?: return null }
    return angle(points[0], points[1], points[2])
}

private fun angle(a: PointF, b: PointF, c: PointF): Double {
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
