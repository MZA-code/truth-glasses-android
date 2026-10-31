package io.github.mzacode.truthglasses.sandbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private const val CHIPS = 5

// Maquette façon YouTube : filtres en haut, puis des vidéos (miniature 16:9, avatar, titre).
// Les pubs ont une ligne « libellé · marque » sous le titre et un bouton d'action.
@Composable
internal fun VideoMockup(ads: List<Ad>, posts: List<Post>, label: String, cta: String) {
    val feed = interleave(ads.map { it.copy(label = label) }, posts, postsBetween = 2)
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Chips() }
        items(feed) { item ->
            when (item) {
                is Post -> VideoItem(item)
                is Ad -> VideoAd(item, cta)
                Banner -> Unit
            }
        }
    }
}

@Composable
private fun Chips() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(CHIPS) { Bar(64.dp, 30.dp) }
    }
}

@Composable
private fun VideoItem(post: Post) {
    Column(Modifier.fillMaxWidth().feedItem()) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            FakeImage(post.author, Modifier.matchParentSize())
            // Durée de la vidéo
            Bar(40.dp, 18.dp, Modifier.align(Alignment.BottomEnd).padding(8.dp))
        }
        Row(Modifier.padding(12.dp)) {
            Avatar(36.dp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(post.text, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(post.author, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun VideoAd(ad: Ad, cta: String) {
    Column(Modifier.fillMaxWidth().feedItem()) {
        FakeImage(ad.brand, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
        Row(Modifier.padding(12.dp)) {
            Avatar(36.dp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(ad.text, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                // Libellé dans son propre texte, séparé de la marque
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ad.label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Text(" · ", style = MaterialTheme.typography.bodySmall)
                    Text(ad.brand, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        CtaButton(cta, Modifier.padding(horizontal = 12.dp).fillMaxWidth())
    }
}
