package io.github.mzacode.truthglasses.detect

// Une pub qui entre par le bas montre d'abord son en-tête : son message est choisi sans la
// légende. Quand le reste arrive, le bloc grandit au même endroit et doit garder ce message
// plutôt que d'en changer en cours de scroll.
object CoverContinuity {
    private const val MIN_HORIZONTAL_OVERLAP = 0.8
    private const val MIN_VERTICAL_OVERLAP = 0.5

    // shown : blocs affichés au scan précédent (décalés par le scroll) et leur message
    fun previousMessage(bounds: Bounds, shown: List<Pair<Bounds, String>>): String? =
        shown
            .mapNotNull { (previous, message) ->
                val overlap = previous.intersect(bounds) ?: return@mapNotNull null
                val sameColumn = overlap.width >= MIN_HORIZONTAL_OVERLAP * minOf(previous.width, bounds.width)
                val sameBlock = overlap.height >= MIN_VERTICAL_OVERLAP * minOf(previous.height, bounds.height)
                if (sameColumn && sameBlock) overlap.area to message else null
            }
            .maxByOrNull { it.first }
            ?.second
}
