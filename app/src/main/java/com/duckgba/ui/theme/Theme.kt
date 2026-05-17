package com.duckgba.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val DuckYellow = Color(0xFFFFD400)
private val DuckYellowDim = Color(0xFFCFAB00)
private val SurfaceBlack = Color(0xFF000000)
private val SurfacePanel = Color(0xFF111111)
private val SurfaceCard = Color(0xFF1A1A1A)
private val OnSurface = Color(0xFFEDEDED)
private val OnSurfaceVariant = Color(0xFFBABABA)
private val DividerColor = Color(0xFF262626)

private val DuckgbaColorScheme = darkColorScheme(
    primary = DuckYellow,
    onPrimary = SurfaceBlack,
    primaryContainer = DuckYellowDim,
    onPrimaryContainer = SurfaceBlack,
    secondary = DuckYellow,
    onSecondary = SurfaceBlack,
    background = SurfaceBlack,
    onBackground = OnSurface,
    surface = SurfaceBlack,
    onSurface = OnSurface,
    surfaceVariant = SurfacePanel,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceContainer = SurfaceCard,
    surfaceContainerHigh = SurfaceCard,
    surfaceContainerHighest = SurfaceCard,
    outline = DividerColor,
    outlineVariant = DividerColor,
    error = Color(0xFFE57373),
    onError = SurfaceBlack
)

private val DuckgbaTypography = Typography(
    displayLarge = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
)

@Composable
fun DuckgbaTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SurfaceBlack.toArgb()
            window.navigationBarColor = SurfaceBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }
    MaterialTheme(
        colorScheme = DuckgbaColorScheme,
        typography = DuckgbaTypography,
        content = content
    )
}
