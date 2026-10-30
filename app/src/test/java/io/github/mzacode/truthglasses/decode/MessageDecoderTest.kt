package io.github.mzacode.truthglasses.decode

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageDecoderTest {
    private val catalog = MessageCatalog(
        byTheme = mapOf(
            Theme.CREDIT to listOf("TU VAS PAYER TROIS FOIS LE PRIX", "ENDETTE-TOI", "SIGNE SANS LIRE"),
            Theme.SALE to listOf("TU N'EN AS PAS BESOIN", "VITE, AVANT DE RÉFLÉCHIR"),
            Theme.GENERIC to listOf("OBÉIS", "CONSOMME", "NE PENSE PAS")
        ),
        short = listOf("DORS", "PAYE")
    )

    @Test
    fun `each theme is recognized in French and English`() {
        val samples = mapOf(
            Theme.CREDIT to listOf("crédit immobilier à taux fixe", "personal loan in minutes"),
            Theme.SALE to listOf("soldes d'hiver sur tout le magasin", "big summer sale today"),
            Theme.FREE to listOf("essai gratuit de 30 jours", "start your free trial"),
            Theme.INSURANCE to listOf("votre mutuelle santé", "car insurance quote"),
            Theme.GAMBLING to listOf("paris sportifs en direct", "online poker tournament"),
            Theme.BEAUTY to listOf("nouveau parfum pour elle", "the best makeup for summer"),
            Theme.CAR to listOf("la nouvelle voiture électrique", "test drive the new suv"),
            Theme.APP to listOf("télécharge l'application", "download the app now"),
            Theme.SUBSCRIPTION to listOf("abonne-toi à la chaîne", "sign up for our newsletter"),
            Theme.TECH to listOf("un ordinateur portable léger", "the new laptop is here"),
            Theme.TRAVEL to listOf("séjour au soleil tout compris", "book your hotel tonight"),
            Theme.FOOD to listOf("pizza livrée chez toi", "fast food delivery"),
            Theme.INVEST to listOf("investis en bourse", "buy stocks today")
        )
        assertEquals(Theme.entries - Theme.GENERIC, samples.keys.toList())
        samples.forEach { (theme, texts) ->
            texts.forEach { assertEquals(it, theme, Themes.detect(it)) }
        }
    }

    @Test
    fun `first matching rule wins`() {
        assertEquals(Theme.CREDIT, Themes.detect("un crédit pour ta voiture"))
    }

    @Test
    fun `no theme gives a generic message`() {
        assertEquals(Theme.GENERIC, Themes.detect("une photo de mon chien"))
        assertEquals(Theme.GENERIC, Themes.detect("just a nice picture of my dog"))
        val message = MessageDecoder.decode("une photo de mon chien", "com.example", 400f, catalog)
        assertTrue(message in catalog.messages(Theme.GENERIC))
    }

    @Test
    fun `wide block picks from its theme`() {
        val message = MessageDecoder.decode("crédit taeg 2,9 %", "com.example", 400f, catalog)
        assertTrue(message in catalog.messages(Theme.CREDIT))
    }

    @Test
    fun `choice is stable for the same ad`() {
        val texts = List(20) { "crédit numéro $it" }
        texts.forEach { text ->
            val first = MessageDecoder.decode(text, "com.example", 400f, catalog)
            repeat(5) { assertEquals(first, MessageDecoder.decode(text, "com.example", 400f, catalog)) }
        }
        // Le choix varie bien d'une pub à l'autre
        val distinct = texts.map { MessageDecoder.decode(it, "com.example", 400f, catalog) }.toSet()
        assertTrue(distinct.size > 1)
    }

    @Test
    fun `narrow block keeps only short messages of its theme`() {
        repeat(20) {
            val message = MessageDecoder.decode("crédit numéro $it", "com.example", 150f, catalog)
            assertTrue(message, message in listOf("ENDETTE-TOI", "SIGNE SANS LIRE"))
        }
    }

    @Test
    fun `narrow block falls back on short messages when its theme has none`() {
        repeat(20) {
            val message = MessageDecoder.decode("soldes numéro $it", "com.example", 150f, catalog)
            assertTrue(message, message in catalog.short)
        }
    }

    @Test
    fun `short rule limits length and word size`() {
        assertTrue(MessageDecoder.isShort("TAIS-TOI"))
        assertTrue(MessageDecoder.isShort("NE PENSE PAS"))
        assertTrue(!MessageDecoder.isShort("CONTINUE DE SCROLLER"))
        assertTrue(!MessageDecoder.isShort("RÉFLÉCHIS"))
    }

    @Test
    fun `real messages exist for every theme in both languages`() {
        listOf("values", "values-fr").forEach { folder ->
            val arrays = readArrays(File("src/main/res/$folder/messages.xml"))
            Theme.entries.forEach { theme ->
                val name = "msg_" + theme.name.lowercase()
                assertTrue("$folder/$name", arrays[name].orEmpty().isNotEmpty())
            }
            val short = arrays["msg_short"].orEmpty()
            assertTrue(short.isNotEmpty())
            short.forEach { assertTrue("$folder: $it", MessageDecoder.isShort(it)) }
        }
    }

    @Test
    fun `sandbox ads cover every theme`() {
        val ads = readArrays(File("src/main/res/values/sandbox_feed.xml")).getValue("sandbox_ads")
        val themes = ads.map { Themes.detect(it.replace('|', ' ').lowercase()) }.toSet()
        assertEquals((Theme.entries - Theme.GENERIC).toSet(), themes)
    }

    private fun readArrays(file: File): Map<String, List<String>> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = document.getElementsByTagName("string-array")
        return (0 until nodes.length).associate { index ->
            val array = nodes.item(index) as org.w3c.dom.Element
            val items = array.getElementsByTagName("item")
            array.getAttribute("name") to (0 until items.length).map {
                items.item(it).textContent.replace("\\'", "'")
            }
        }
    }
}
