package com.saik.expensemanager.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme =
  darkColorScheme(
    primary = WarmGoldPrimary,
    secondary = WarmGoldSecondary,
    tertiary = WarmGoldTertiary,
    background = WarmDarkBackground,
    surface = WarmDarkSurface,
    onPrimary = WarmDarkOnPrimary,
    onBackground = WarmGoldSecondary,
    onSurface = WarmGoldSecondary,
    outline = Color(0xFF534333),
    outlineVariant = Color(0xFF3B2F23)
  )

private val DeepBlueDarkColorScheme = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    secondary = Color(0xFFBBC7DB),
    tertiary = Color(0xFFD6BEE1),
    background = BlueDarkBackground,
    surface = BlueDarkSurface,
    onPrimary = Color(0xFF003258),
    onBackground = Color(0xFFD1E4FF),
    onSurface = Color(0xFFD1E4FF),
    outline = Color(0xFF8C9199),
    outlineVariant = Color(0xFF42474E)
)

private val DeepBlueLightColorScheme = lightColorScheme(
    primary = BluePrimary,
    secondary = Color(0xFF535F70),
    tertiary = Color(0xFF6B5778),
    background = BlueBackground,
    surface = BlueSurface,
    onPrimary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF001C2E),
    onSurface = Color(0xFF001C2E),
    outline = Color(0xFF72777F),
    outlineVariant = Color(0xFFC2C7CF)
)

private val ForestGreenDarkColorScheme = darkColorScheme(
    primary = Color(0xFF86D999),
    secondary = Color(0xFFB9CCB8),
    tertiary = Color(0xFFA1CED1),
    background = GreenDarkBackground,
    surface = GreenDarkSurface,
    onPrimary = Color(0xFF00391C),
    onBackground = Color(0xFFE1E3DA),
    onSurface = Color(0xFFE1E3DA),
    outline = Color(0xFF8B9389),
    outlineVariant = Color(0xFF414941)
)

private val ForestGreenLightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    secondary = Color(0xFF526350),
    tertiary = Color(0xFF396567),
    background = GreenBackground,
    surface = GreenSurface,
    onPrimary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF191C19),
    onSurface = Color(0xFF191C19),
    outline = Color(0xFF727970),
    outlineVariant = Color(0xFFC1C9BE)
)

private val RoyalPurpleDarkColorScheme = darkColorScheme(
    primary = Color(0xFFF3ADFF),
    secondary = Color(0xFFD2C1D7),
    tertiary = Color(0xFFF2B9BB),
    background = PurpleDarkBackground,
    surface = PurpleDarkSurface,
    onPrimary = Color(0xFF4C1666),
    onBackground = Color(0xFFE9E0E8),
    onSurface = Color(0xFFE9E0E8),
    outline = Color(0xFF968E99),
    outlineVariant = Color(0xFF4C444D)
)

private val RoyalPurpleLightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    secondary = Color(0xFF685A6B),
    tertiary = Color(0xFF805253),
    background = PurpleBackground,
    surface = PurpleSurface,
    onPrimary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1D1A1E),
    onSurface = Color(0xFF1D1A1E),
    outline = Color(0xFF7F7480),
    outlineVariant = Color(0xFFD0C3D0)
)

private val LightColorScheme =
  lightColorScheme(
    primary = WarmClayPrimary,
    secondary = WarmClaySecondary,
    tertiary = WarmClayTertiary,
    background = WarmClayBackground,
    surface = WarmClaySurface,
    surfaceVariant = WarmClaySurfaceVariant,
    onPrimary = WarmClayOnPrimary,
    onBackground = WarmClayOnBackground,
    onSurface = WarmClayOnBackground,
    outline = Color(0xFF857361),
    outlineVariant = Color(0xFFD7C2B1)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  colorTheme: String = "WARM_CLAY",
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      else -> {
          if (darkTheme) {
              when (colorTheme) {
                  "DEEP_BLUE" -> DeepBlueDarkColorScheme
                  "FOREST_GREEN" -> ForestGreenDarkColorScheme
                  "ROYAL_PURPLE" -> RoyalPurpleDarkColorScheme
                  else -> DarkColorScheme
              }
          } else {
              when (colorTheme) {
                  "DEEP_BLUE" -> DeepBlueLightColorScheme
                  "FOREST_GREEN" -> ForestGreenLightColorScheme
                  "ROYAL_PURPLE" -> RoyalPurpleLightColorScheme
                  else -> LightColorScheme
              }
          }
      }
    }

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      window.statusBarColor = Color.Transparent.toArgb()
      val insetsController = WindowCompat.getInsetsController(window, view)
      insetsController.isAppearanceLightStatusBars = !darkTheme
      insetsController.isAppearanceLightNavigationBars = !darkTheme
    }
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
