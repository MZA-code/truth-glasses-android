package io.github.mzacode.truthglasses.sandbox

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

private const val STORIES = 6

// Maquette façon Instagram : stories, puis des posts avec en-tête, image carrée et légende.
// Les pubs ont le libellé sous le pseudo et un bandeau d'action sous l'image.
@Composable
internal fun PhotoMockup(ads: List<Ad>, posts: List<Post>, label: String, cta: String) {
    val feed = interleave(ads.map { it.copy(label = label) }, posts, postsBetween = 2)
    LazyColumn(Modifier.fillMaxSize()) {
        item { Stories() }
        items(feed) { item ->
            when (item) {
                is Post -> PhotoPost(item.author, item.text, null, null)
                is Ad -> PhotoPost(item.brand, item.text, item.label, cta)
                Banner -> Unit
            }
        }
    }
}

@Composable
private fun Stories() {
    Row(
        Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(STORIES) { Avatar(56.dp) }
    }
}

@Composable
private fun PhotoPost(author: String, caption: String, label: String?, cta: String?) {
    Column(Modifier.fillMaxWidth().feedItem().padding(bottom = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(32.dp)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(author, style = MaterialTheme.typography.titleSmall)
                if (label != null) Text(label, style = MaterialTheme.typography.labelSmall)
            }
            Bar(18.dp, 6.dp)
        }
        FakeImage(author, Modifier.fillMaxWidth().aspectRatio(1f))
        if (cta != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(cta, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                Bar(8.dp, 12.dp)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            repeat(3) { Avatar(22.dp) }
            Spacer(Modifier.weight(1f))
            Avatar(22.dp)
        }
        Bar(90.dp, modifier = Modifier.padding(horizontal = 12.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(author) }
                append(' ')
                append(caption)
            },
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
