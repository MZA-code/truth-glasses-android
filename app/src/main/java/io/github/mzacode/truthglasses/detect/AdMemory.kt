package io.github.mzacode.truthglasses.detect

// Mémoire courte des pubs déjà recouvertes, pour continuer à les recouvrir quand leur libellé
// sort de l'écran, et leur garder le même message. Elle ne garde que des empreintes (hash des
// textes longs du bloc, voir AdDetector) et le message choisi, jamais le texte lu à l'écran.
// Le service la vide quand on quitte l'appli cible.
class AdMemory(private val capacity: Int = DEFAULT_CAPACITY) {
    private class Entry(var fingerprint: Set<Int>, val message: String)

    private val entries = ArrayDeque<Entry>()

    val isEmpty get() = entries.isEmpty()

    fun knows(fingerprint: Set<Int>) = find(fingerprint) != null

    // Message déjà choisi pour cette pub, sinon celui de decode, retenu pour la suite
    fun messageFor(fingerprint: Set<Int>, decode: () -> String): String {
        if (fingerprint.isEmpty()) return decode()
        val known = find(fingerprint)
        if (known != null) {
            // Plus on voit de la pub, plus son empreinte est complète
            known.fingerprint += fingerprint
            entries.remove(known)
            entries.addLast(known)
            return known.message
        }
        val message = decode()
        entries.addLast(Entry(fingerprint, message))
        if (entries.size > capacity) entries.removeFirst()
        return message
    }

    fun clear() = entries.clear()

    // Un seul texte long en commun suffit : deux blocs différents n'en partagent pas
    private fun find(fingerprint: Set<Int>) =
        entries.lastOrNull { entry -> fingerprint.any { it in entry.fingerprint } }

    private companion object {
        const val DEFAULT_CAPACITY = 32
    }
}
