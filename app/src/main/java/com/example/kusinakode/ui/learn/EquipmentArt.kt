package com.example.kusinakode.ui.learn

import androidx.annotation.DrawableRes
import com.example.kusinakode.R

/**
 * Photography for the kitchen equipment named in the dish dataset.
 * 
 * Generated - do not hand-edit. Keys are the dataset's own `tools`
 * strings, normalised so punctuation and casing differences in the sheet
 * ("Strainer/colander" vs "Strainer or colander") still resolve. A tool
 * with no picture returns null and the caller falls back to an icon.
 */
object EquipmentArt {

    private val byName: Map<String, Int> = mapOf(
        "bamboo skewers" to R.drawable.equip_bamboo_skewers,
        "basting brush" to R.drawable.equip_basting_brush,
        "charcoal grill or barbecue grill" to R.drawable.equip_barbecue_grill,
        "charcoal roasting pit or large outdoor grill" to R.drawable.equip_charcoal_roasting_pit,
        "chopping board" to R.drawable.equip_chopping_board,
        "chopping board and cleaver knife" to R.drawable.equip_cleaver_knife,
        "deep frying pan wok or large pot" to R.drawable.equip_deep_frying_pan,
        "flat griddle or frying pan kawali" to R.drawable.equip_kawali,
        "food thermometer" to R.drawable.equip_food_thermometer,
        "fork or pastry cutter" to R.drawable.equip_pastry_cutter,
        "fresh bamboo tube" to R.drawable.equip_fresh_bamboo_tube,
        "frying pan" to R.drawable.equip_frying_pan_or_skillet,
        "frying pan or skillet" to R.drawable.equip_frying_pan_or_skillet,
        "grill or charcoal grill" to R.drawable.equip_charcoal_grill,
        "heat resistant gloves" to R.drawable.equip_heat_resistant_gloves,
        "heavy bottomed pot" to R.drawable.equip_heavy_bottomed_pot,
        "kitchen twine or food safe wire" to R.drawable.equip_kitchen_twine,
        "knife" to R.drawable.equip_knife,
        "knife and chopping board" to R.drawable.equip_chopping_board,
        "knife or dough cutter" to R.drawable.equip_dough_cutter,
        "large basin" to R.drawable.equip_large_basin,
        "large cooking pot" to R.drawable.equip_large_cooking_pot,
        "large cooking pot with lid" to R.drawable.equip_large_cooking_pot_with_lid,
        "large knife or cleaver" to R.drawable.equip_cleaver_knife,
        "large mixing bowl" to R.drawable.equip_large_mixing_bowl,
        "large roasting spit metal rod" to R.drawable.equip_roasting_spit_metal_rod_called_as_rotisserie_spit,
        "large saucepan or pot" to R.drawable.equip_large_saucepan,
        "large stockpot" to R.drawable.equip_large_stockpot,
        "leaves" to R.drawable.equip_banana_leaves,
        "long wooden stick sursur" to R.drawable.equip_long_wooden_stick_sursur,
        "measuring cups and spoons" to R.drawable.equip_measuring_cups_and_spoons,
        "mixing bowl" to R.drawable.equip_mixing_bowl,
        "mortar and pestle" to R.drawable.equip_mortar_and_pestle,
        "mortar and pestle or food processor" to R.drawable.equip_mortar_and_pestle,
        "open fire hearth or wood fired stove" to R.drawable.equip_wood_fired_stove,
        "optional pressure cooker" to R.drawable.equip_pressure_cooker,
        "oven" to R.drawable.equip_oven,
        "pressure cooker" to R.drawable.equip_pressure_cooker,
        "roasting rack" to R.drawable.equip_roasting_rack,
        "rolling pin" to R.drawable.equip_rolling_pin,
        "sizzling plate cast iron plate" to R.drawable.equip_sizzling_plate,
        "small mixing bowl" to R.drawable.equip_small_mixing_bowl,
        "small pot" to R.drawable.equip_small_pot,
        "small saucepan or pan" to R.drawable.equip_small_saucepan,
        "strainer" to R.drawable.equip_strainer,
        "strainer or colander" to R.drawable.equip_colander,
        "strainer colander" to R.drawable.equip_colander,
        "tongs or firewood handling tools" to R.drawable.equip_firewood_handling_tools,
        "wire rack and baking tray" to R.drawable.equip_wire_rack_and_baking_tray,
        "wok or deep frying pan" to R.drawable.equip_wok,
    )

    /** Drawable for [name], or null when we have no picture for it. */
    @DrawableRes
    fun forName(name: String): Int? =
        byName[name.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()]
}
