package io.github.mzacode.truthglasses

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mzacode.truthglasses.sandbox.Ad
import io.github.mzacode.truthglasses.sandbox.Banner
import io.github.mzacode.truthglasses.sandbox.FakeImage
import io.github.mzacode.truthglasses.sandbox.ForumMockup
import io.github.mzacode.truthglasses.sandbox.PhotoMockup
import io.github.mzacode.truthglasses.sandbox.Post
import io.github.mzacode.truthglasses.sandbox.VideoMockup
import io.github.mzacode.truthglasses.sandbox.feedItem
import io.github.mzacode.truthglasses.sandbox.interleave
import io.github.mzacode.truthglasses.sandbox.parseAds
import io.github.mzacode.truthglasses.sandbox.parsePosts
import io.github.mzacode.truthglasses.ui.TruthGlassesTheme

// Fil générique : une pub tous les 3 posts, chaque thème apparaît une fois
private const val NORMAL_POSTS_BETWEEN_ADS = 2
private const val BANNER_POSITION = 8

// Les 13 pubs sont réparties entre les maquettes pour que chaque thème y apparaisse aussi
private val PHOTO_ADS = 0..4
private val VIDEO_ADS = 5..8
private val FORUM_ADS = 9..12

private enum class SandboxTab(@StringRes val title: Int) {
    FEED(R.string.sandbox_tab_feed),
    PHOTOS(R.string.sandbox_tab_photos),
    VIDEOS(R.string.sandbox_tab_videos),
    FORUM(R.string.sandbox_tab_forum)
}

class SandboxActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TruthGlassesTheme {
                Surface(Modifier.fillMaxSize()) {
                    var tab by rememberSaveable { mutableStateOf(SandboxTab.FEED) }
                    val ads = parseAds(stringArrayResource(R.array.sandbox_ads))
                    val posts = parsePosts(stringArrayResource(R.array.sandbox_posts))
                    val cta = stringResource(R.string.sandbox_cta)
                    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                        PrimaryScrollableTabRow(selectedTabIndex = tab.ordinal, edgePadding = 0.dp) {
                            SandboxTab.entries.forEach {
                                Tab(
                                    selected = it == tab,
                                    onClick = { tab = it },
                                    text = { Text(stringResource(it.title)) }
                                )
                            }
                        }
                        when (tab) {
                            SandboxTab.FEED -> GenericFeed(ads, posts)
                            SandboxTab.PHOTOS -> PhotoMockup(
                                ads.slice(PHOTO_ADS), posts, stringResource(R.string.sandbox_label_photos), cta
                            )
                            SandboxTab.VIDEOS -> VideoMockup(
                                ads.slice(VIDEO_ADS), posts, stringResource(R.string.sandbox_label_videos), cta
                            )
                            SandboxTab.FORUM -> ForumMockup(
                                ads.slice(FORUM_ADS), posts, stringResource(R.string.sandbox_label_forum), cta
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GenericFeed(ads: List<Ad>, posts: List<Post>) {
    val feed = interleave(ads, posts, NORMAL_POSTS_BETWEEN_ADS).apply { add(BANNER_POSITION, Banner) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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

@Composable
private fun FeedCard(title: String, text: String, label: String?) {
    Column(
        Modifier
            .fillMaxWidth()
            .feedItem()
            .padding(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (label != null) Text(label, style = MaterialTheme.typography.labelSmall)
        Text(text, Modifier.padding(vertical = 8.dp))
        FakeImage(title, Modifier.fillMaxWidth().height(180.dp))
    }
}

@Composable
private fun NarrowBanner() {
    Column(
        Modifier
            .width(160.dp)
            .height(240.dp)
            .feedItem()
            .background(Color(0xFFFFE082))
            .padding(8.dp)
    ) {
        // Texte noir fixe : le fond jaune ne change pas en mode sombre
        Text(stringResource(R.string.sandbox_banner_label), color = Color.Black, style = MaterialTheme.typography.labelSmall)
        Text(stringResource(R.string.sandbox_banner_text), color = Color.Black, style = MaterialTheme.typography.titleMedium)
    }
}
