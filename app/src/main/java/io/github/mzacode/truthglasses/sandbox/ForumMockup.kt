package io.github.mzacode.truthglasses.sandbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

// Maquette façon Reddit : des cartes avec la ligne communauté, le titre, une image et la
// barre de votes. Les pubs ont le libellé à côté de la marque et un bouton d'action.
@Composable
internal fun ForumMockup(ads: List<Ad>, posts: List<Post>, label: String, cta: String) {
    val feed = interleave(ads.map { it.copy(label = label) }, posts, postsBetween = 2)
    LazyColumn(Modifier.fillMaxSize()) {
        items(feed) { item ->
            when (item) {
                is Post -> ForumCard(item.author, item.text, null, null)
                is Ad -> ForumCard(item.brand, item.text, item.label, cta)
                Banner -> Unit
            }
        }
    }
}

@Composable
private fun ForumCard(source: String, title: String, label: String?, cta: String?) {
    Column(Modifier.fillMaxWidth().feedItem()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(24.dp)
                Text(source, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelLarge)
                if (label != null) {
                    Text(label, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelSmall)
                } else {
                    // Ancienneté du post
                    Bar(28.dp, 8.dp, Modifier.padding(start = 8.dp))
                }
            }
            Text(title, Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.titleMedium)
            FakeImage(
                source,
                Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(12.dp))
            )
            if (cta != null) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Spacer(Modifier.weight(1f))
                    CtaButton(cta)
                }
            }
            // Votes, commentaires, partage
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Bar(88.dp, 32.dp)
                Bar(56.dp, 32.dp)
                Bar(64.dp, 32.dp)
            }
        }
        HorizontalDivider()
    }
}
