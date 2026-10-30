package io.github.mzacode.truthglasses.detect

data class AppProfile(
    val packageName: String,
    val displayName: String,
    // Libellé EXACT d'un nœud (texte ou contentDescription), après trim
    val sponsorLabels: List<Regex>,
    // Date de vérification avec un dump, null = pas encore vérifié
    val verifiedOn: String?
)

object AppProfiles {
    fun all(ownPackageName: String): List<AppProfile> =
        listOf(sandbox(ownPackageName), INSTAGRAM, REDDIT, YOUTUBE)

    private fun sandbox(ownPackageName: String) = AppProfile(
        packageName = ownPackageName,
        displayName = "Sandbox",
        sponsorLabels = listOf(
            Regex("^sponsoris[ée]e?$", RegexOption.IGNORE_CASE),
            Regex("^sponsored$", RegexOption.IGNORE_CASE)
        ),
        verifiedOn = "2026-10-30"
    )

    // Libellés pas encore vérifiés : ils seront relevés avec un dump au jalon M5.
    // D'ici là, ces profils ne détectent rien.
    private val INSTAGRAM = AppProfile("com.instagram.android", "Instagram", emptyList(), null)
    private val REDDIT = AppProfile("com.reddit.frontpage", "Reddit", emptyList(), null)
    private val YOUTUBE = AppProfile("com.google.android.youtube", "YouTube", emptyList(), null)
}
