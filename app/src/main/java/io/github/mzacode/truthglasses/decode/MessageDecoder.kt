package io.github.mzacode.truthglasses.decode

import android.content.res.Resources
import io.github.mzacode.truthglasses.R

class MessageCatalog(private val byTheme: Map<Theme, List<String>>, val short: List<String>) {
    fun messages(theme: Theme): List<String> = byTheme[theme].orEmpty()

    companion object {
        fun from(resources: Resources): MessageCatalog {
            val byTheme = Theme.entries.associateWith { theme ->
                resources.getStringArray(arrayRes(theme)).toList()
            }
            return MessageCatalog(byTheme, resources.getStringArray(R.array.msg_short).toList())
        }

        private fun arrayRes(theme: Theme) = when (theme) {
            Theme.CREDIT -> R.array.msg_credit
            Theme.SALE -> R.array.msg_sale
            Theme.FREE -> R.array.msg_free
            Theme.INSURANCE -> R.array.msg_insurance
            Theme.GAMBLING -> R.array.msg_gambling
            Theme.BEAUTY -> R.array.msg_beauty
            Theme.CAR -> R.array.msg_car
            Theme.APP -> R.array.msg_app
            Theme.SUBSCRIPTION -> R.array.msg_subscription
            Theme.TECH -> R.array.msg_tech
            Theme.TRAVEL -> R.array.msg_travel
            Theme.FOOD -> R.array.msg_food
            Theme.INVEST -> R.array.msg_invest
            Theme.GENERIC -> R.array.msg_generic
        }
    }
}

object MessageDecoder {
    const val NARROW_WIDTH_DP = 200f
    private const val SHORT_MAX_LENGTH = 16
    private const val SHORT_MAX_WORD_LETTERS = 8

    fun decode(adText: String, packageName: String, widthDp: Float, catalog: MessageCatalog): String {
        val theme = Themes.detect(adText)
        var candidates = catalog.messages(theme).ifEmpty { catalog.messages(Theme.GENERIC) }
        if (widthDp < NARROW_WIDTH_DP) {
            candidates = candidates.filter(::isShort).ifEmpty { catalog.short }
        }
        // Choix stable : les blocs sont recréés à chaque scan, une même pub doit garder son message.
        // Le masque évite la valeur absolue négative de Int.MIN_VALUE.
        val seed = (packageName + adText).hashCode() and Int.MAX_VALUE
        return candidates[seed % candidates.size]
    }

    fun isShort(message: String): Boolean =
        message.length <= SHORT_MAX_LENGTH &&
            message.split(' ').all { word -> word.count(Char::isLetter) <= SHORT_MAX_WORD_LETTERS }
}
