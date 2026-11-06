package com.example.automatesmartbot.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.example.automatesmartbot.ui.theme.FixedCanvasTheme

/** Authentication UI follows the approved white/blue mockup even if
 * KarirMate is set to DARK/AUTO and the phone has night mode enabled.
 * The rest of the application keeps its existing theme preference.
 */
@Composable
fun AuthLightTheme(content: @Composable () -> Unit) {
    val activity = LocalContext.current.findAuthActivity()
    DisposableEffect(activity) {
        val controller = activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView)
        }
        val previousStatusIcons = controller?.isAppearanceLightStatusBars
        val previousNavigationIcons = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = true
        controller?.isAppearanceLightNavigationBars = true
        onDispose {
            if (previousStatusIcons != null) controller?.isAppearanceLightStatusBars = previousStatusIcons
            if (previousNavigationIcons != null) controller?.isAppearanceLightNavigationBars = previousNavigationIcons
        }
    }
    FixedCanvasTheme(dark = false, content = content)
}

private tailrec fun Context.findAuthActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findAuthActivity()
    else -> null
}
