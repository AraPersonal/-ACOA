package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val HighDensityColorScheme = darkColorScheme(
  primary = HighDensityPrimary,
  onPrimary = HighDensityOnPrimary,
  primaryContainer = HighDensityPrimary,
  onPrimaryContainer = HighDensityOnPrimary,
  secondary = HighDensityGreen,
  background = HighDensityBg,
  onBackground = HighDensityText,
  surface = HighDensitySurface,
  onSurface = HighDensityText,
  surfaceVariant = HighDensitySurfaceVariant,
  onSurfaceVariant = HighDensityText,
  outline = HighDensityOutline,
  error = HighDensityError
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme for high density design
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = HighDensityColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
