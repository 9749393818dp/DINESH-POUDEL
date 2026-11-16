package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = DarkPrimary,
  onPrimary = EmeraldDeep,
  primaryContainer = EmeraldPrimary,
  onPrimaryContainer = Color.White,
  secondary = DarkSecondary,
  onSecondary = EmeraldDeep,
  tertiary = MarigoldAccent,
  background = DarkBackground,
  surface = DarkSurface,
  onBackground = Color(0xFFE8EFEA),
  onSurface = Color(0xFFE8EFEA),
)

private val LightColorScheme = lightColorScheme(
  primary = EmeraldPrimary,
  onPrimary = Color.White,
  primaryContainer = EmeraldContainer,
  onPrimaryContainer = OnEmeraldContainer,
  secondary = EmeraldSecondary,
  onSecondary = Color.White,
  secondaryContainer = EmeraldContainer,
  onSecondaryContainer = OnEmeraldContainer,
  tertiary = MarigoldAccent,
  tertiaryContainer = MarigoldContainer,
  background = MintBackground,
  onBackground = Color(0xFF1A221D),
  surface = CardWhite,
  onSurface = Color(0xFF1A221D),
  surfaceVariant = Color(0xFFEBF0EC),
  onSurfaceVariant = Color(0xFF49544C),
  outline = SoftBorder,
  error = ExpenseRed,
)

@Composable
fun MeruHisabTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content,
  )
}
