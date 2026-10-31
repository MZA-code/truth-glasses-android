package io.github.mzacode.truthglasses.detect

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppProfilesTest {
    private val profiles = AppProfiles.all("io.github.mzacode.truthglasses").associateBy { it.packageName }

    private fun matches(packageName: String, text: String) =
        profiles.getValue(packageName).sponsorLabels.any { it.matches(text.trim()) }

    @Test
    fun `real apps match the labels seen on screen`() {
        assertTrue(matches("com.instagram.android", "Sponsorisée"))
        assertTrue(matches("com.google.android.youtube", "Sponsorisée"))
        assertTrue(matches("com.reddit.frontpage", "Publicité"))
    }

    @Test
    fun `labels only match a whole node`() {
        assertFalse(matches("com.reddit.frontpage", "Voici une publicité pour un café"))
        assertFalse(matches("com.instagram.android", "Publication sponsorisée par la marque"))
        assertFalse(matches("com.google.android.youtube", "Publicité"))
    }

    @Test
    fun `every real app profile is verified`() {
        profiles.values.forEach { assertNotNull(it.displayName, it.verifiedOn) }
    }

    @Test
    fun `sandbox matches every mockup label`() {
        listOf("Sponsorisée", "Publicité", "Sponsored", "Sponsorisé").forEach {
            assertTrue(it, matches("io.github.mzacode.truthglasses", it))
        }
    }
}
