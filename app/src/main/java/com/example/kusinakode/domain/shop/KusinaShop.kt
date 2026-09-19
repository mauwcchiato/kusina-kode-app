package com.example.kusinakode.domain.shop

enum class ShopKind { DOCUMENTARY, ENCYCLOPEDIA, AVATAR }

enum class AvatarSlot { FRAME, CHARACTER }

data class ShopItem(
    val id: String,
    val kind: ShopKind,
    val title: String,
    val subtitle: String,
    val coinCost: Int,
    /** Public watch link; only opened after a KK unlock. */
    val watchUrl: String? = null,
    val article: String? = null,
    val emoji: String = "",
    val slot: AvatarSlot? = null,
    /** Short chips for pantry pages — keep the shelf itself quiet. */
    val tags: List<String> = emptyList()
)

/**
 * KK shop catalogue. Prices must match api/lib/shop.php.
 * Documentary links are public YouTube searches (we do not host the films).
 */
object KusinaShop {

    private fun reel(
        id: String,
        title: String,
        subtitle: String,
        coinCost: Int,
        query: String
    ) = ShopItem(
        id = id,
        kind = ShopKind.DOCUMENTARY,
        title = title,
        subtitle = subtitle,
        coinCost = coinCost,
        watchUrl = "https://www.youtube.com/results?search_query=$query"
    )

    val documentaries = listOf(
        reel("doc_adobo", "The Story of Adobo", "Philippines · Savory Vinegar Pork", 8, "filipino+adobo+history+documentary"),
        reel("doc_sinigang", "The Story of Sinigang", "Philippines · Sour Tamarind Soup", 8, "sinigang+filipino+soup+documentary"),
        reel("doc_paksiw", "The Story of Paksiw", "Visayas · Vinegar-Braised Catch", 8, "paksiw+filipino+documentary"),
        reel("doc_sisig", "The Story of Sisig", "Luzon · Sizzling Chopped Pork", 8, "sisig+pampanga+documentary"),
        reel("doc_mechado", "The Story of Mechado", "Philippines · Tomato Beef Stew", 8, "mechado+filipino+documentary"),
        reel("doc_menudo", "The Story of Menudo", "Philippines · Tomato Pork Stew", 8, "menudo+filipino+documentary"),
        reel("doc_caldereta", "The Story of Caldereta", "Philippines · Rich Liver Stew", 8, "caldereta+filipino+documentary"),
        reel("doc_afritada", "The Story of Afritada", "Philippines · Tomato Chicken Stew", 8, "afritada+filipino+documentary"),
        reel("doc_humba", "The Story of Humba", "Visayas · Sweet Braised Pork", 8, "humba+filipino+documentary"),
        reel("doc_pinikpikan", "The Story of Pinikpikan", "Luzon · Smoky Highland Chicken", 10, "pinikpikan+cordillera+documentary"),
        reel("doc_inabraw", "The Story of Inabraw", "Luzon · Bagoong Vegetable Broth", 8, "inabraw+dinengdeng+documentary"),
        reel("doc_pinuneg", "The Story of Pinuneg", "Luzon · Cordillera Blood Sausage", 10, "pinuneg+filipino+documentary"),
        reel("doc_sinursur", "The Story of Sinursur", "Luzon · Coconut Fish Stew", 8, "sinursur+ilocano+documentary"),
        reel("doc_binakol", "The Story of Binakol", "Visayas · Coconut Chicken Soup", 8, "binakol+filipino+documentary"),
        reel("doc_la_paz_batchoy", "The Story of La Paz Batchoy", "Visayas · Rich Pork Noodles", 8, "la+paz+batchoy+iloilo+documentary"),
        reel("doc_inasal", "The Story of Inasal", "Visayas · Charcoal Grilled Chicken", 8, "inasal+bacolod+documentary"),
        reel("doc_kansi", "The Story of Kansi", "Visayas · Sour Beef Shank", 8, "kansi+bacolod+documentary"),
        reel("doc_piaya", "The Story of Piaya", "Visayas · Muscovado Flatbread", 8, "piaya+bacolod+documentary"),
        reel("doc_tiyula_itum", "The Story of Tiyula Itum", "Mindanao · Black Coconut Soup", 12, "tiyula+itum+tausug+documentary"),
        reel("doc_piaparan", "The Story of Piaparan", "Mindanao · Spiced Coconut Chicken", 12, "piaparan+maranao+documentary"),
        reel("doc_pastil", "The Story of Pastil", "Mindanao · Wrapped Chicken Rice", 8, "pastil+maguindanao+documentary"),
        reel("doc_sinuglaw", "The Story of Sinuglaw", "Mindanao · Grill And Kinilaw", 8, "sinuglaw+filipino+documentary"),
        reel("doc_kulma", "The Story of Kulma", "Mindanao · Coconut Curry Stew", 10, "kulma+maguindanao+documentary"),
        reel("doc_satti", "The Story of Satti", "Mindanao · Skewers In Sauce", 8, "satti+zamboanga+documentary"),
        reel("doc_pigar_pigar", "The Story of Pigar-Pigar", "Luzon · Sizzling Beef Plate", 8, "pigar+pigar+dagupan+documentary"),
        reel("doc_bulalo", "The Story of Bulalo", "Luzon · Beef Bone Broth", 8, "bulalo+batangas+documentary"),
        reel("doc_chicharon_carcar", "The Story of Chicharon Carcar", "Visayas · Crunchy Pork Rinds", 8, "chicharon+carcar+cebu+documentary"),
        reel("doc_lechon", "The Story of Lechon", "Visayas · Whole Roast Pig", 10, "cebu+lechon+documentary"),
        reel("doc_bicol_express", "The Story of Bicol Express", "Luzon · Spicy Coconut Pork", 8, "bicol+express+documentary"),
        reel("doc_laing", "The Story of Laing", "Luzon · Taro Leaves In Coconut", 8, "laing+bicol+documentary")
    )

    val encyclopedia = listOf(
        ShopItem(
            id = "pan_calamansi",
            kind = ShopKind.ENCYCLOPEDIA,
            title = "Calamansi",
            subtitle = "The little citrus of every table",
            coinCost = 6,
            emoji = "🟢",
            tags = listOf("Citrus", "Sawsawan"),
            article = "Calamansi is the squeeze that finishes a Filipino plate — brighter than lemon, " +
                "softer than lime. Cooks use the juice in sawsawan, on grilled fish, and in " +
                "marinades so meat stays tender. The whole fruit, rind and all, is often crushed " +
                "into iced calamansi juice. In the KODEX it sits beside every sour dish as the " +
                "island's own citrus, not an imported one."
        ),
        ShopItem(
            id = "pan_bagoong",
            kind = ShopKind.ENCYCLOPEDIA,
            title = "Bagoong",
            subtitle = "Fermented shrimp or fish paste",
            coinCost = 6,
            emoji = "🦐",
            tags = listOf("Umami", "Coast"),
            article = "Bagoong is time, salt, and tiny shrimp or fish left to become umami. " +
                "It is the backbone of pinakbet and kare-kare's sawsawan. Colour runs from " +
                "pink-grey to deep brick depending on the coast. A spoonful replaces a stock cube — " +
                "the KODEX treats it as a living ingredient, not a condiment on the side."
        ),
        ShopItem(
            id = "pan_suka",
            kind = ShopKind.ENCYCLOPEDIA,
            title = "Sukang Tuba",
            subtitle = "Coconut sap vinegar",
            coinCost = 6,
            emoji = "🥥",
            tags = listOf("Vinegar", "Coconut"),
            article = "Sukang tuba starts as sweet coconut sap and becomes the sharp vinegar behind " +
                "adobo, paksiw, and atchara. Each island's suka tastes a little different — some " +
                "cloudy, some amber, some with a whisper of coconut. It is how Filipino kitchens " +
                "kept food safe before refrigeration, and why so many heritage dishes taste bright " +
                "instead of heavy."
        ),
        ShopItem(
            id = "pan_pandan",
            kind = ShopKind.ENCYCLOPEDIA,
            title = "Pandan",
            subtitle = "The vanilla of the tropics",
            coinCost = 6,
            emoji = "🌿",
            tags = listOf("Aroma", "Kakanin"),
            article = "Pandan leaves are tied into a knot and dropped into rice or syrup so the " +
                "steam carries a grassy-sweet perfume. Desserts like buko pandan and many " +
                "kakanin would taste unfinished without it. In the KODEX pantry it is the " +
                "leaf that makes a kitchen smell like home before the food even hits the plate."
        ),
        ShopItem(
            id = "pan_ube",
            kind = ShopKind.ENCYCLOPEDIA,
            title = "Ube",
            subtitle = "Purple yam of fiestas",
            coinCost = 8,
            emoji = "🟣",
            tags = listOf("Yam", "Fiesta"),
            article = "Ube (Dioscorea alata) is the purple yam behind halo-halo, ube halaya, and " +
                "modern bakery trends. Traditional cooks steam and mash it with milk and sugar " +
                "until it turns into a dense jam. The colour is natural when the variety is true " +
                "ube — the KODEX page exists so the ingredient is not mistaken for taro or " +
                "food dye alone."
        ),
        ShopItem(
            id = "pan_achuete",
            kind = ShopKind.ENCYCLOPEDIA,
            title = "Achuete",
            subtitle = "Annatto — colour and a quiet earthiness",
            coinCost = 6,
            emoji = "🧡",
            tags = listOf("Color", "Earth"),
            article = "Achuete seeds are soaked in oil or water to stain kare-kare, pancit, and " +
                "empanada fillings that warm sunset orange. The flavour is mild — nutty, slightly " +
                "peppery — so it is used as much for colour as for taste. Spanish-era kitchens " +
                "adopted it from the Americas; Filipino cooks made it a Tuesday-night habit."
        )
    )

    val frames = listOf(
        ShopItem(
            id = "av_lei",
            kind = ShopKind.AVATAR,
            title = "Sampaguita Frame",
            subtitle = "A wreath of sampaguita blossoms",
            coinCost = 80,
            emoji = "🌼",
            slot = AvatarSlot.FRAME
        ),
        ShopItem(
            id = "av_toque",
            kind = ShopKind.AVATAR,
            title = "Okir Frame",
            subtitle = "Okir carved in warm wood",
            coinCost = 120,
            emoji = "🌀",
            slot = AvatarSlot.FRAME
        ),
        ShopItem(
            id = "av_apron",
            kind = ShopKind.AVATAR,
            title = "Gumamela Frame",
            subtitle = "A wreath of red gumamela",
            coinCost = 150,
            emoji = "🌺",
            slot = AvatarSlot.FRAME
        ),
        ShopItem(
            id = "av_kawali",
            kind = ShopKind.AVATAR,
            title = "Ginto Frame",
            subtitle = "Gold filigree and a beaded rim",
            coinCost = 180,
            emoji = "✨",
            slot = AvatarSlot.FRAME
        ),
        ShopItem(
            id = "av_salakot",
            kind = ShopKind.AVATAR,
            title = "Abaca Frame",
            subtitle = "A braid of abaca rope",
            coinCost = 100,
            emoji = "🪢",
            slot = AvatarSlot.FRAME
        ),
        ShopItem(
            id = "av_bougainvillea",
            kind = ShopKind.AVATAR,
            title = "Bougainvillea Frame",
            subtitle = "A wreath of pink bougainvillea",
            coinCost = 120,
            emoji = "🌸",
            slot = AvatarSlot.FRAME
        ),
        ShopItem(
            id = "av_pearl",
            kind = ShopKind.AVATAR,
            title = "Pearl Frame",
            subtitle = "A ring of South Sea pearls",
            coinCost = 160,
            emoji = "💮",
            slot = AvatarSlot.FRAME
        ),
        ShopItem(
            id = "av_tarsier",
            kind = ShopKind.AVATAR,
            title = "Tarsier Frame",
            subtitle = "Vines, leaves, and a tarsier",
            coinCost = 140,
            emoji = "🐒",
            slot = AvatarSlot.FRAME
        ),
        ShopItem(
            id = "av_sarimanok",
            kind = ShopKind.AVATAR,
            title = "Agila Frame",
            subtitle = "Crest, wings, and talons of the agila",
            coinCost = 200,
            emoji = "🦅",
            slot = AvatarSlot.FRAME
        ),
        ShopItem(
            id = "av_araw",
            kind = ShopKind.AVATAR,
            title = "Araw Frame",
            subtitle = "The eight-rayed sun of the flag",
            coinCost = 180,
            emoji = "☀",
            slot = AvatarSlot.FRAME
        ),
        ShopItem(
            id = "av_yakan",
            kind = ShopKind.AVATAR,
            title = "Yakan Frame",
            subtitle = "Yakan diamonds in deep red",
            coinCost = 160,
            emoji = "🧵",
            slot = AvatarSlot.FRAME
        ),
        ShopItem(
            id = "av_butanding",
            kind = ShopKind.AVATAR,
            title = "Butanding Frame",
            subtitle = "A bubble ring and a butanding",
            coinCost = 140,
            emoji = "🦈",
            slot = AvatarSlot.FRAME
        ),
        ShopItem(
            id = "av_vinta",
            kind = ShopKind.AVATAR,
            title = "Vinta Frame",
            subtitle = "A vinta with striped sails",
            coinCost = 160,
            emoji = "⛵",
            slot = AvatarSlot.FRAME
        ),
        ShopItem(
            id = "av_jeepney",
            kind = ShopKind.AVATAR,
            title = "Jeepney Frame",
            subtitle = "A jeepney at the lower rim",
            coinCost = 120,
            emoji = "🚌",
            slot = AvatarSlot.FRAME
        ),
        ShopItem(
            id = "av_parol",
            kind = ShopKind.AVATAR,
            title = "Parol Frame",
            subtitle = "A gold parol with colored tassels",
            coinCost = 140,
            emoji = "🌟",
            slot = AvatarSlot.FRAME
        )
    )

    val characters = listOf(
        ShopItem(
            id = "avc_lola",
            kind = ShopKind.AVATAR,
            title = "Lola Kusinera",
            subtitle = "The kitchen's first teacher",
            coinCost = 120,
            emoji = "👵",
            slot = AvatarSlot.CHARACTER
        ),
        ShopItem(
            id = "avc_farmer",
            kind = ShopKind.AVATAR,
            title = "Ani the Farmer",
            subtitle = "She who brings palay to the pot",
            coinCost = 100,
            emoji = "🌾",
            slot = AvatarSlot.CHARACTER
        ),
        ShopItem(
            id = "avc_barong",
            kind = ShopKind.AVATAR,
            title = "Barong Chef",
            subtitle = "Host of the formal table",
            coinCost = 140,
            emoji = "👔",
            slot = AvatarSlot.CHARACTER
        ),
        ShopItem(
            id = "avc_fiesta",
            kind = ShopKind.AVATAR,
            title = "Fiesta Cook",
            subtitle = "Cook of the town fiesta",
            coinCost = 120,
            emoji = "🌸",
            slot = AvatarSlot.CHARACTER
        ),
        ShopItem(
            id = "avc_kusinero",
            kind = ShopKind.AVATAR,
            title = "Kusinero",
            subtitle = "The household cook",
            coinCost = 60,
            emoji = "👨",
            slot = AvatarSlot.CHARACTER
        ),
        ShopItem(
            id = "avc_kusinera",
            kind = ShopKind.AVATAR,
            title = "Kusinera",
            subtitle = "The household cook",
            coinCost = 60,
            emoji = "👩",
            slot = AvatarSlot.CHARACTER
        ),
        ShopItem(
            id = "avc_chef_m",
            kind = ShopKind.AVATAR,
            title = "Punong Kusinero",
            subtitle = "Executive chef of the kitchen",
            coinCost = 160,
            emoji = "👨",
            slot = AvatarSlot.CHARACTER
        ),
        ShopItem(
            id = "avc_chef_f",
            kind = ShopKind.AVATAR,
            title = "Punong Kusinera",
            subtitle = "Executive chef of the kitchen",
            coinCost = 160,
            emoji = "👩",
            slot = AvatarSlot.CHARACTER
        ),
        ShopItem(
            id = "avc_crew_m",
            kind = ShopKind.AVATAR,
            title = "Kitchen Crew",
            subtitle = "Line cook of the brigade",
            coinCost = 80,
            emoji = "🧑",
            slot = AvatarSlot.CHARACTER
        ),
        ShopItem(
            id = "avc_crew_f",
            kind = ShopKind.AVATAR,
            title = "Kitchen Crew",
            subtitle = "Line cook of the brigade",
            coinCost = 80,
            emoji = "🧑",
            slot = AvatarSlot.CHARACTER
        ),
        ShopItem(
            id = "avc_farmer_m",
            kind = ShopKind.AVATAR,
            title = "Magsasaka",
            subtitle = "He who brings palay to the pot",
            coinCost = 100,
            emoji = "🌾",
            slot = AvatarSlot.CHARACTER
        ),
        ShopItem(
            id = "avc_lolo",
            kind = ShopKind.AVATAR,
            title = "Lolo Kusinero",
            subtitle = "Keeper of the family recipes",
            coinCost = 120,
            emoji = "👴",
            slot = AvatarSlot.CHARACTER
        ),
        ShopItem(
            id = "avc_igorot_m",
            kind = ShopKind.AVATAR,
            title = "Igorot Kusinero",
            subtitle = "Cook of the Cordillera highlands",
            coinCost = 140,
            emoji = "⛰",
            slot = AvatarSlot.CHARACTER
        ),
        ShopItem(
            id = "avc_igorot_f",
            kind = ShopKind.AVATAR,
            title = "Igorot Kusinera",
            subtitle = "Cook of the Cordillera highlands",
            coinCost = 140,
            emoji = "⛰",
            slot = AvatarSlot.CHARACTER
        ),
        ShopItem(
            id = "avc_maranao_m",
            kind = ShopKind.AVATAR,
            title = "Maranao Kusinero",
            subtitle = "Cook of Lanao's royal kitchen",
            coinCost = 140,
            emoji = "🕌",
            slot = AvatarSlot.CHARACTER
        ),
        ShopItem(
            id = "avc_maranao_f",
            kind = ShopKind.AVATAR,
            title = "Maranao Kusinera",
            subtitle = "Cook of Lanao's royal kitchen",
            coinCost = 140,
            emoji = "🕌",
            slot = AvatarSlot.CHARACTER
        ),
        ShopItem(
            id = "avc_dahlia",
            kind = ShopKind.AVATAR,
            title = "Dahlia",
            subtitle = "Vendor of the palengke",
            coinCost = 120,
            emoji = "🌸",
            slot = AvatarSlot.CHARACTER
        ),
        ShopItem(
            id = "avc_aly",
            kind = ShopKind.AVATAR,
            title = "Aly",
            subtitle = "Technical lead of the kusina",
            coinCost = 200,
            emoji = "🔓",
            slot = AvatarSlot.CHARACTER
        ),
        ShopItem(
            id = "avc_mau",
            kind = ShopKind.AVATAR,
            title = "Mau",
            subtitle = "Steward of the puzzle",
            coinCost = 200,
            emoji = "🧩",
            slot = AvatarSlot.CHARACTER
        ),
        ShopItem(
            id = "avc_kyla",
            kind = ShopKind.AVATAR,
            title = "Kyla",
            subtitle = "Keeper of the cultural record",
            coinCost = 200,
            emoji = "📚",
            slot = AvatarSlot.CHARACTER
        ),
        ShopItem(
            id = "avc_yow",
            kind = ShopKind.AVATAR,
            title = "Yow",
            subtitle = "Guardian of quality",
            coinCost = 200,
            emoji = "🔎",
            slot = AvatarSlot.CHARACTER
        ),
        ShopItem(
            id = "avc_ian",
            kind = ShopKind.AVATAR,
            title = "Ian",
            subtitle = "Adviser of the kitchen",
            coinCost = 200,
            emoji = "🍛",
            slot = AvatarSlot.CHARACTER
        ),
        ShopItem(
            id = "avc_belen",
            kind = ShopKind.AVATAR,
            title = "Belen",
            subtitle = "Regular of the kitchen",
            coinCost = 200,
            emoji = "🍛",
            slot = AvatarSlot.CHARACTER
        )
    )

    val avatars: List<ShopItem> = frames + characters

    /** Thematic reels replaced by dish posters; kept so old tickets still resolve. */
    private val retiredReels = listOf(
        reel("doc_palengke", "Palengke: The Wet Market", "Where every dish begins", 12, "filipino+wet+market+palengke+documentary"),
        reel("doc_mindanao", "Flavors of the Royal South", "Mindanao · Maranao & Tausug kitchens", 12, "mindanao+filipino+cuisine+documentary")
    )

    val all: List<ShopItem> = documentaries + encyclopedia + avatars + retiredReels

    fun item(id: String): ShopItem? = all.firstOrNull { it.id == id }

    fun prices(): Map<String, Int> = all.associate { it.id to it.coinCost }
}
