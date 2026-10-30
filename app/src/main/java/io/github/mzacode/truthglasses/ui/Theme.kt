package io.github.mzacode.truthglasses.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import io.github.mzacode.truthglasses.R

val Anton = FontFamily(Font(R.font.anton_regular))

// Noir et gris clair, comme les blocs posés sur les pubs
private val LightColors = lightColorScheme(
    primary = Color(0xFF111111),
    onPrimary = Color(0xFFF2F2F2),
    background = Color(0xFFF2F2F2),
    surface = Color(0xFFF2F2F2),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFE2E2E2)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF2F2F2),
    onPrimary = Color(0xFF111111),
    background = Color(0xFF111111),
    surface = Color(0xFF111111),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHighest = Color(0xFF333333)
)

@Composable
fun TruthGlassesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
