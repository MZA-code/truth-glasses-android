package io.github.mzacode.truthglasses.sandbox

// Contenu fictif du bac à sable, lu dans res/values/sandbox_feed.xml
internal sealed interface FeedItem
internal data class Post(val author: String, val text: String) : FeedItem
internal data class Ad(val label: String, val brand: String, val text: String) : FeedItem
internal data object Banner : FeedItem

internal fun parsePosts(raw: Array<String>) = raw.map {
    val (author, text) = it.split('|', limit = 2)
    Post(author, text)
}

internal fun parseAds(raw: Array<String>) = raw.map {
    val (label, brand, text) = it.split('|', limit = 3)
    Ad(label, brand, text)
}

// Une pub après chaque série de postsBetween posts normaux
internal fun interleave(ads: List<Ad>, posts: List<Post>, postsBetween: Int): MutableList<FeedItem> {
    val feed = mutableListOf<FeedItem>()
    var postIndex = 0
    ads.forEach { ad ->
        repeat(postsBetween) { feed += posts[postIndex++ % posts.size] }
        feed += ad
    }
    return feed
}
