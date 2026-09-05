package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = LgesGoldDark,
    secondary = LgesSlateDark,
    tertiary = LgesGoldDark,
    background = Color(0xFF121416),
    surface = Color(0xFF1E2124),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFFEFF1F4),
    onSurface = Color(0xFFEFF1F4),
    surfaceVariant = Color(0xFF2D3133),
    onSurfaceVariant = Color(0xFFC6C5D4),
    error = Color(0xFFBA1A1A)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = LgesNavy, // #000666 Deep Navy
    secondary = LgesSlate, // #006876 Secondary Cyan
    tertiary = LgesGold, // #FFD700 Accent Gold
    background = LightSlate, // #F7F9FC Cool off-white
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFF191C1E),
    onSurface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFFE0E3E6),
    onSurfaceVariant = Color(0xFF454652),
    error = Color(0xFFBA1A1A)
  )



@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disabling dynamic color by default to preserve cohesive LGES brand styling
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
