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
    // Les vraies applis arrivent au jalon M5, une fois leurs libellés vérifiés avec un dump
    fun all(ownPackageName: String): List<AppProfile> = listOf(sandbox(ownPackageName))

    private fun sandbox(ownPackageName: String) = AppProfile(
        packageName = ownPackageName,
        displayName = "Sandbox",
        sponsorLabels = listOf(
            Regex("^sponsoris[ée]e?$", RegexOption.IGNORE_CASE),
            Regex("^sponsored$", RegexOption.IGNORE_CASE)
        ),
        verifiedOn = "2026-10-30"
    )
}
