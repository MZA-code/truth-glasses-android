package io.github.mzacode.truthglasses.sandbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Briques des maquettes : des formes grises à la place des logos, des icônes et des compteurs.
// On reproduit la mise en page des applis, sans leur identité visuelle.

// isTraversalGroup donne au post son propre nœud d'accessibilité : sans lui, ses textes
// seraient des enfants directs de la liste et le bloc ne couvrirait que le libellé
internal fun Modifier.feedItem() = semantics { isTraversalGroup = true }

@Composable
internal fun Avatar(size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
}

@Composable
internal fun Bar(width: Dp, height: Dp = 10.dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(width, height)
            .clip(RoundedCornerShape(height / 2))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@Composable
internal fun FakeImage(seed: String, modifier: Modifier = Modifier) {
    Box(modifier.background(Brush.verticalGradient(gradientFor(seed))))
}

@Composable
internal fun CtaButton(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.onPrimary,
        style = MaterialTheme.typography.labelLarge
    )
}

// Dégradé stable, dérivé du nom affiché
private fun gradientFor(seed: String): List<Color> {
    val hue = (seed.hashCode() and Int.MAX_VALUE) % 360
    return listOf(Color.hsv(hue.toFloat(), 0.5f, 0.9f), Color.hsv(hue.toFloat(), 0.8f, 0.5f))
}
