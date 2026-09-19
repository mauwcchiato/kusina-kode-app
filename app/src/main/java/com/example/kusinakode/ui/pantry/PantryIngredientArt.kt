package com.example.kusinakode.ui.pantry

import androidx.annotation.DrawableRes
import com.example.kusinakode.R

/**
 * Photography for every ingredient the baul can hand out.
 *
 * Keys are [com.example.kusinakode.domain.pantry.IngredientCatalog] ids, so a
 * draw never falls back to the painted jar. Several ids share one photograph
 * the same way [com.example.kusinakode.ui.learn.IngredientArt] shares pictures
 * across the dish dataset's ingredient strings — but only where the two really
 * do look alike (dried fish/hibe, banana leaves used as a wrapper). A stand-in
 * has to be the same creature or the card teaches the wrong thing.
 *
 * Three of them broke that rule and have been corrected: catfish was showing
 * an eel, goby/frog a tilapia, and batwan a kamias. The real pictures existed
 * all along in the web panel's ingredient images and simply had not been
 * brought across — a Kodex card is a teaching surface, so the wrong animal on
 * it is a wrong answer, not a cosmetic slip.
 */
object PantryIngredientArt {

    private val byId: Map<String, Int> = mapOf(
        "ing_all_purpose_flour" to R.drawable.ing_all_purposeflour,
        "ing_annatto_oil" to R.drawable.ing_annatooil,
        "ing_annatto_powder_seeds" to R.drawable.ing_annatoseeds,
        "ing_bagoong_fermented_fish_paste" to R.drawable.ing_bagoong,
        "ing_bamboo_skewers" to R.drawable.ing_bambooskewers,
        "ing_bamboo_tube" to R.drawable.ing_bambootube,
        "ing_banana_blossoms_dried" to R.drawable.ing_bananablossoms,
        "ing_banana_leaves" to R.drawable.ing_bananaleaves,
        "ing_bay_leaves" to R.drawable.ing_bayleaves,
        "ing_beef" to R.drawable.ing_beef,
        "ing_beef_broth_stock" to R.drawable.ing_beefbroth,
        "ing_beef_cube_powder" to R.drawable.ing_beefcubes,
        "ing_bell_peppers_red_green" to R.drawable.ing_bellpeppers,
        "ing_bitter_melon" to R.drawable.ing_bittermelon,
        "ing_black_beans_salted_tausi" to R.drawable.ing_tausi,
        "ing_bok_choy_shanghai" to R.drawable.ing_bokchoy,
        "ing_bread_banana_or_other_leaves" to R.drawable.ing_bananaleaves,
        "ing_butter" to R.drawable.ing_butter,
        "ing_cabbage" to R.drawable.ing_cabbage,
        "ing_calamansi_juice_fruit" to R.drawable.ing_calamansi,
        "ing_carrots" to R.drawable.ing_carrots,
        "ing_catfish" to R.drawable.ing_catfish,
        "ing_celery" to R.drawable.ing_celery,
        "ing_chayote" to R.drawable.ing_chayote,
        "ing_cheddar_cheese" to R.drawable.ing_cheddarcheese,
        "ing_chicharon" to R.drawable.ing_chicharon,
        "ing_chicken" to R.drawable.ing_chicken,
        "ing_chicken_broth_stock" to R.drawable.ing_chickenbroth,
        "ing_chicken_liver" to R.drawable.ing_chickenliver,
        "ing_chili_flakes" to R.drawable.ing_chiliflakes,
        "ing_chili_paste_sambal" to R.drawable.ing_sambal,
        "ing_chili_pepper_birds_eye" to R.drawable.ing_chilipepper_bird_s_eye,
        "ing_chili_pepper_long_green_siling" to R.drawable.ing_chilipepper_long_green,
        "ing_chili_pepper_red_hot" to R.drawable.ing_chilipepper_red_hot,
        "ing_cinnamon" to R.drawable.ing_cinnamon,
        "ing_coconut_cream" to R.drawable.ing_coconutcream,
        "ing_coconut_meat" to R.drawable.ing_coconutmeat,
        "ing_coconut_milk" to R.drawable.ing_coconutmilk,
        "ing_coconut_water_young" to R.drawable.ing_coconutwater,
        "ing_cooking_oil" to R.drawable.ing_cookingoil,
        "ing_cornstarch" to R.drawable.ing_cornstarch,
        "ing_cucumber" to R.drawable.ing_cucumber,
        "ing_curry_powder" to R.drawable.ing_currypowder,
        "ing_daikon_radish" to R.drawable.ing_daikonradish,
        "ing_dried_fish_shrimp" to R.drawable.ing_hibe,
        "ing_dried_shrimp_hibe" to R.drawable.ing_hibe,
        "ing_eel" to R.drawable.ing_eel,
        "ing_egg_whole" to R.drawable.ing_egg,
        "ing_eggplant_chinese_indian" to R.drawable.ing_eggplant,
        "ing_fish_tilapia" to R.drawable.ing_tilapia,
        "ing_fish_sauce_patis" to R.drawable.ing_fishsauce,
        "ing_gabi_leaves_taro_leaves_fresh" to R.drawable.ing_gabileaves,
        "ing_garlic" to R.drawable.ing_garlic,
        "ing_ginger" to R.drawable.ing_ginger,
        // Bulig is a stubby freshwater fish and palaka is a frog — neither is
        // an eel. Tilapia is the nearest river fish in the set until goby and
        // frog art exists.
        "ing_goby_frog_river_ingredients" to R.drawable.ing_goby_frog,
        "ing_green_mango" to R.drawable.ing_greenmango,
        "ing_green_olives" to R.drawable.ing_greenolives,
        "ing_green_peas" to R.drawable.ing_greenpeas,
        "ing_guava" to R.drawable.ing_guava,
        "ing_hotdogs" to R.drawable.ing_hotdog,
        "ing_jackfruit_unripe" to R.drawable.ing_jackfruit,
        "ing_jute_leaves_saluyot" to R.drawable.ing_juteleaves,
        "ing_kamias" to R.drawable.ing_kamias,
        "ing_batwan_sour_fruit" to R.drawable.ing_batwan,
        "ing_knorr_liquid_seasoning" to R.drawable.ing_knorrliquidseasoning,
        "ing_ladys_choice_mayonnaise" to R.drawable.ing_lady_schoicemayonnaise,
        "ing_lemon" to R.drawable.ing_lemon,
        "ing_lemongrass" to R.drawable.ing_lemongrass,
        "ing_lime" to R.drawable.ing_lime,
        "ing_liver_spread" to R.drawable.ing_liverspread,
        "ing_margarine" to R.drawable.ing_margarine,
        "ing_miki_noodles_fresh" to R.drawable.ing_mikinoodles,
        "ing_milkfish_bangus" to R.drawable.ing_milkfish,
        "ing_muscovado_sugar" to R.drawable.ing_muscovadosugar,
        "ing_okra" to R.drawable.ing_okra,
        "ing_onion" to R.drawable.ing_onion,
        "ing_onion_leeks_scallions" to R.drawable.ing_onionleeks,
        "ing_onion_powder" to R.drawable.ing_onionpowder,
        "ing_oyster_sauce" to R.drawable.ing_oystersauce,
        "ing_palapa_maranao_condiment" to R.drawable.ing_palapa,
        "ing_paprika" to R.drawable.ing_paprika,
        "ing_peanut_butter" to R.drawable.ing_peanutbutter,
        "ing_pechay_native" to R.drawable.ing_pechay,
        "ing_pepper_ground_black_white" to R.drawable.ing_pepper,
        "ing_peppercorns_whole_black" to R.drawable.ing_peppercorns,
        "ing_pig_blood" to R.drawable.ing_pigblood,
        "ing_pig_face_maskara" to R.drawable.ing_pigface,
        "ing_pig_intestines_casing" to R.drawable.ing_pigintestines,
        "ing_pig_whole_dressed" to R.drawable.ing_pig_whole,
        "ing_pineapple_juice" to R.drawable.ing_pineapplejuice,
        "ing_pork" to R.drawable.ing_pork,
        "ing_pork_belly" to R.drawable.ing_porkbelly,
        "ing_pork_liver" to R.drawable.ing_porkliver,
        "ing_pork_offal_mixed" to R.drawable.ing_porkoffal_mixed,
        "ing_pork_rind_skin" to R.drawable.ing_porkskin,
        "ing_potatoes" to R.drawable.ing_potatoes,
        "ing_rice_white_cooked" to R.drawable.ing_rice,
        "ing_rice_wine_tapuy" to R.drawable.ing_ricewine,
        "ing_sakurab_shallots_native_shallots" to R.drawable.ing_sakurab,
        "ing_salt" to R.drawable.ing_salt,
        "ing_santol" to R.drawable.ing_santol,
        "ing_sesame_seeds" to R.drawable.ing_sesameseeds,
        "ing_shallots" to R.drawable.ing_shallots,
        "ing_shortening" to R.drawable.ing_shortening,
        "ing_shrimp_with_head" to R.drawable.ing_shrimp,
        "ing_shrimp_paste_bagoong_alamang" to R.drawable.ing_shrimppaste,
        "ing_sinigang_mix_powdered" to R.drawable.ing_sinigangmix,
        "ing_soy_sauce" to R.drawable.ing_soysauce,
        "ing_speck_etag_smoked_pork" to R.drawable.ing_speck,
        "ing_spring_onions" to R.drawable.ing_springonions,
        "ing_string_beans_sitaw" to R.drawable.ing_stringbeans,
        "ing_sugar" to R.drawable.ing_sugar,
        "ing_tamarind_young_sampaloc" to R.drawable.ing_tamarind_young,
        "ing_tomato" to R.drawable.ing_tomato,
        "ing_tomato_paste" to R.drawable.ing_tomatopaste,
        "ing_tomato_sauce" to R.drawable.ing_tomatosauce,
        "ing_tuna_fresh" to R.drawable.ing_tuna,
        "ing_turmeric_powder" to R.drawable.ing_turmeric,
        "ing_vegetable_oil" to R.drawable.ing_vegetableoil,
        "ing_vinegar_white_cane_coconut" to R.drawable.ing_vinegar,
        "ing_water" to R.drawable.ing_water,
        "ing_water_spinach_kangkong" to R.drawable.ing_waterspinach,
        "ing_white_pepper" to R.drawable.ing_whitepepper,
    )

    /** Drawable for the catalog [id], or null when we have no picture for it. */
    @DrawableRes
    fun forId(id: String): Int? = byId[id]
}
