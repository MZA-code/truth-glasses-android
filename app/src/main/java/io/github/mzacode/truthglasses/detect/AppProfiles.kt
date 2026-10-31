package io.github.mzacode.truthglasses.detect

data class AppProfile(
    val packageName: String,
    val displayName: String,
    // Libellé EXACT d'un nœud (texte ou contentDescription), après trim
    val sponsorLabels: List<Regex>,
    // Date de vérification sur l'appareil, null = pas encore vérifié
    val verifiedOn: String?
)

object AppProfiles {
    fun all(ownPackageName: String): List<AppProfile> =
        listOf(sandbox(ownPackageName), INSTAGRAM, REDDIT, YOUTUBE)

    // Reprend les libellés des vraies applis, affichés par les maquettes (sandbox_label_*)
    private fun sandbox(ownPackageName: String) = AppProfile(
        packageName = ownPackageName,
        displayName = "Sandbox",
        sponsorLabels = listOf(
            Regex("^sponsoris[ée]e?$", RegexOption.IGNORE_CASE),
            Regex("^sponsored$", RegexOption.IGNORE_CASE),
            Regex("^publicité$", RegexOption.IGNORE_CASE)
        ),
        verifiedOn = "2026-10-30"
    )

    // Libellés relevés à l'écran sur un téléphone en français, le 2026-10-31.
    // Les libellés anglais ne sont pas encore vérifiés : un téléphone en anglais ne détecte rien.
    private val INSTAGRAM = AppProfile(
        "com.instagram.android", "Instagram",
        listOf(Regex("^sponsorisée$", RegexOption.IGNORE_CASE)), "2026-10-31"
    )
    private val REDDIT = AppProfile(
        "com.reddit.frontpage", "Reddit",
        listOf(Regex("^publicité$", RegexOption.IGNORE_CASE)), "2026-10-31"
    )
    private val YOUTUBE = AppProfile(
        "com.google.android.youtube", "YouTube",
        listOf(Regex("^sponsorisée$", RegexOption.IGNORE_CASE)), "2026-10-31"
    )
}
