package io.github.dimitrysaf.asko.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Material 3 Expressive theme. Colors come from the wallpaper (Material You);
 * minSdk is 31, so dynamic color is always available.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AskoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    MaterialExpressiveTheme(
        colorScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context),
        motionScheme = MotionScheme.expressive(),
        shapes = AskoShapes,
        typography = AskoTypography,
        content = content,
    )
}
