package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VoxLightColorScheme =
  lightColorScheme(
    primary = CoralPrimary,
    onPrimary = Color.White,
    primaryContainer = CoralPrimaryContainer,
    onPrimaryContainer = OnCoralPrimaryContainer,
    secondary = SlateSecondary,
    onSecondary = Color.White,
    secondaryContainer = SlateSecondaryContainer,
    onSecondaryContainer = OnSlateSecondaryContainer,
    tertiary = EmeraldAccent,
    onTertiary = Color.White,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = OnEmeraldContainer,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = CoralPrimary,
    onError = Color.White,
  )

private val VoxDarkColorScheme =
  darkColorScheme(
    primary = CoralDark,
    onPrimary = Color(0xFF3E0602),
    primaryContainer = CoralDarkContainer,
    onPrimaryContainer = OnCoralDarkContainer,
    secondary = SlateSecondaryDark,
    onSecondary = Color(0xFF1A202C),
    secondaryContainer = SlateSecondaryDarkContainer,
    onSecondaryContainer = OnSlateSecondaryDarkContainer,
    tertiary = EmeraldDark,
    onTertiary = Color(0xFF04301E),
    tertiaryContainer = EmeraldDarkContainer,
    onTertiaryContainer = OnEmeraldDarkContainer,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = CoralDark,
    onError = Color(0xFF3E0602),
  )

@Composable
fun VoxTypeTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) VoxDarkColorScheme else VoxLightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = VoxTypeTypography,
    content = content,
  )
}
