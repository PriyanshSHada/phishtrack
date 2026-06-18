package com.example.phishtrack.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextMuted,
    error = ErrorRed,
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.White
)

@Immutable
data class ExtendedColors(
    val warning: Color,
    val info: Color,
    val success: Color,
    val mediumPriority: Color,
    val errorLight: Color
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        warning = Color.Unspecified,
        info = Color.Unspecified,
        success = Color.Unspecified,
        mediumPriority = Color.Unspecified,
        errorLight = Color.Unspecified
    )
}

@Composable
fun PhishTrackTheme(
  darkTheme: Boolean = true, // Force dark theme as per app design
  dynamicColor: Boolean = false, // Disable dynamic color to maintain specific brand colors
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme
  val extendedColors = ExtendedColors(
      warning = WarningOrange,
      info = InfoBlue,
      success = SuccessGreen,
      mediumPriority = MediumYellow,
      errorLight = ErrorLight
  )

  CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
      MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}
