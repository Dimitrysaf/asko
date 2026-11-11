package io.github.dimitrysaf.asko.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer

/*
 * Unsharp mask on the GPU: enlarging a small image smears every edge over several screen pixels;
 * subtracting a blurred copy from the image puts that edge contrast back. It can't add detail the
 * file doesn't have, but it removes the blur. RuntimeShader (AGSL) needs Android 13; older phones
 * get the plain image.
 */
private const val UNSHARP_MASK_AGSL = """
uniform shader content;
uniform float radius;
uniform float amount;

half4 main(float2 p) {
    half4 c = content.eval(p);
    // 3x3 Gaussian-weighted blur, with taps spread to match how far the image was enlarged.
    half3 edges = content.eval(p + float2(-radius, 0)).rgb + content.eval(p + float2(radius, 0)).rgb
                + content.eval(p + float2(0, -radius)).rgb + content.eval(p + float2(0, radius)).rgb;
    half3 corners = content.eval(p + float2(-radius, -radius)).rgb + content.eval(p + float2(radius, -radius)).rgb
                  + content.eval(p + float2(-radius, radius)).rgb + content.eval(p + float2(radius, radius)).rgb;
    half3 blur = (4.0 * c.rgb + 2.0 * edges + corners) / 16.0;
    return half4(clamp(c.rgb + (c.rgb - blur) * amount, 0.0, c.a), c.a);
}
"""

/** How strongly edges are restored. Higher looks crisper but starts to halo. */
private const val SHARPEN_AMOUNT = 1.4f

/**
 * Sharpens content that is an enlarged [sourcePx]-pixel square image (e.g. ExerciseDB's 180 px
 * GIFs), scaled to how much it is enlarged on screen. No effect when it isn't enlarged.
 */
@Composable
fun Modifier.sharpenEnlarged(sourcePx: Int): Modifier =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) sharpenWithShader(sourcePx) else this

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun Modifier.sharpenWithShader(sourcePx: Int): Modifier {
    val shader = remember { RuntimeShader(UNSHARP_MASK_AGSL) }
    return graphicsLayer {
        // The square image fits the shorter side, so that side tells how much it was enlarged.
        val enlargement = minOf(size.width, size.height) / sourcePx
        renderEffect = if (enlargement > 1.5f) {
            shader.setFloatUniform("radius", enlargement * 0.5f)
            shader.setFloatUniform("amount", SHARPEN_AMOUNT)
            RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
        } else {
            null
        }
    }
}
