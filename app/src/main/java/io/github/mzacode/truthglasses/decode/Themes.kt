package io.github.mzacode.truthglasses.decode

// Chaque thème correspond au string-array "msg_<nom en minuscules>" de res/values/messages.xml
enum class Theme {
    CREDIT, SALE, FREE, INSURANCE, GAMBLING, BEAUTY, CAR, APP,
    SUBSCRIPTION, TECH, TRAVEL, FOOD, INVEST, GENERIC
}

object Themes {
    // Règles reprises de l'extension, dans cet ordre : la première qui correspond gagne.
    // Les mots-clés sont bilingues pour reconnaître une pub anglaise sur un téléphone en français.
    private val rules = listOf(
        Theme.CREDIT to """cr[ée]dit|pr[êe]t|financement|mensualit|taeg|\bloan|financing|\bapr\b|mortgage""",
        Theme.SALE to """soldes|promo|r[ée]duction|-\s?\d+\s?%|\d+\s?% off|black friday|offre limit|\bsale\b|discount|\bdeals?\b|limited offer""",
        Theme.FREE to """gratuit|offert|\bfree\b|essai|\btrial""",
        Theme.INSURANCE to """assurance|mutuelle|s[ée]curit|alarme|insurance|security|alarm""",
        Theme.GAMBLING to """\bparis?\b|casino|poker|jackpot|loto|lottery|bonus de bienvenue|welcome bonus|\bbet""",
        Theme.BEAUTY to """parfum|perfume|beaut[ée]|beauty|cr[èe]me|cream|anti-[âa]ge|anti-aging|maquillage|makeup|minceur|weight loss|r[ée]gime|\bdiet""",
        Theme.CAR to """voiture|\bcars?\b|\bsuv\b|leasing|\blease\b|\bauto\b|\bmoto""",
        Theme.APP to """t[ée]l[ée]charge|download|appli|\bapp\b|app store|google play|install""",
        Theme.SUBSCRIPTION to """abonne|newsletter|inscri|subscri|sign up""",
        Theme.TECH to """smartphone|iphone|galaxy|ordinateur|laptop|\btv\b|[ée]cran|screen""",
        Theme.TRAVEL to """vacances|voyage|s[ée]jour|h[ôo]tel|\bvols?\b|vacation|holiday|travel|flights?\b""",
        Theme.FOOD to """fast food|burger|pizza|livr[ée]|delivery|soda""",
        Theme.INVEST to """investi|crypto|bourse|trading|rendement|stocks?\b|returns"""
    ).map { (theme, pattern) -> theme to Regex(pattern) }

    // adText est déjà en minuscules (voir AdDetector)
    fun detect(adText: String): Theme =
        rules.firstOrNull { (_, regex) -> regex.containsMatchIn(adText) }?.first ?: Theme.GENERIC
}
