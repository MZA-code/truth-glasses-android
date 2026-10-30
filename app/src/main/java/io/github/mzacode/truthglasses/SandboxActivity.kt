package io.github.mzacode.truthglasses

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

// Une pub tous les 3 posts, assez de posts pour que chaque thème apparaisse une fois
private const val NORMAL_POSTS_BETWEEN_ADS = 2
private const val BANNER_POSITION = 8

private sealed interface FeedItem
private data class Post(val author: String, val text: String) : FeedItem
private data class Ad(val label: String, val brand: String, val text: String) : FeedItem
private data object Banner : FeedItem

class SandboxActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    val feed = buildFeed(
                        stringArrayResource(R.array.sandbox_ads),
                        stringArrayResource(R.array.sandbox_posts)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(feed) { item ->
                            when (item) {
                                is Post -> FeedCard(item.author, item.text, null)
                                is Ad -> FeedCard(item.brand, item.text, item.label)
                                Banner -> NarrowBanner()
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildFeed(ads: Array<String>, posts: Array<String>): List<FeedItem> {
    val feed = mutableListOf<FeedItem>()
    var postIndex = 0
    ads.forEach { ad ->
        repeat(NORMAL_POSTS_BETWEEN_ADS) {
            val (author, text) = posts[postIndex++ % posts.size].split('|', limit = 2)
            feed += Post(author, text)
        }
        val (label, brand, text) = ad.split('|', limit = 3)
        feed += Ad(label, brand, text)
    }
    feed.add(BANNER_POSITION, Banner)
    return feed
}

// isTraversalGroup donne au post son propre nœud d'accessibilité : sans lui, ses textes
// seraient des enfants directs de la liste et le bloc ne couvrirait que le libellé
@Composable
private fun FeedCard(title: String, text: String, label: String?) {
    Column(
        Modifier
            .fillMaxWidth()
            .semantics { isTraversalGroup = true }
            .padding(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (label != null) Text(label, style = MaterialTheme.typography.labelSmall)
        Text(text, Modifier.padding(vertical = 8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Brush.verticalGradient(gradientFor(title)))
        )
    }
}

@Composable
private fun NarrowBanner() {
    Column(
        Modifier
            .width(160.dp)
            .height(240.dp)
            .semantics { isTraversalGroup = true }
            .background(Color(0xFFFFE082))
            .padding(8.dp)
    ) {
        Text(stringResource(R.string.sandbox_banner_label), style = MaterialTheme.typography.labelSmall)
        Text(stringResource(R.string.sandbox_banner_text), style = MaterialTheme.typography.titleMedium)
    }
}

// Dégradé stable, dérivé du nom affiché
private fun gradientFor(title: String): List<Color> {
    val hue = (title.hashCode() and Int.MAX_VALUE) % 360
    return listOf(Color.hsv(hue.toFloat(), 0.5f, 0.9f), Color.hsv(hue.toFloat(), 0.8f, 0.5f))
}
