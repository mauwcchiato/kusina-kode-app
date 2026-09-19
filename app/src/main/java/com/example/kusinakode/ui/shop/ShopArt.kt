package com.example.kusinakode.ui.shop

import androidx.annotation.DrawableRes
import com.example.kusinakode.R

/**
 * Artwork for the shop's avatars and frames.
 *
 * Every image was cropped from the same 1080x1080 window of its source canvas,
 * so a frame and a character drawn at the same size line up without any
 * per-item nudging — the frame rings the character because the art already
 * agrees on where the centre is.
 *
 * Returns null for an id with no art, and callers fall back to the procedural
 * ring / pixel sprite. That keeps anything not yet drawn from rendering blank.
 */
object ShopArt {

    @DrawableRes
    fun character(id: String?): Int? = when (id) {
        "avc_lola" -> R.drawable.avatar_lola_f
        "avc_farmer" -> R.drawable.avatar_farmer_f
        "avc_barong" -> R.drawable.avatar_barong_m
        "avc_fiesta" -> R.drawable.avatar_saya_f
        "avc_kusinero" -> R.drawable.avatar_male
        "avc_kusinera" -> R.drawable.avatar_female
        "avc_chef_m" -> R.drawable.avatar_chef_m
        "avc_chef_f" -> R.drawable.avatar_chef_f
        "avc_crew_m" -> R.drawable.avatar_crew_m
        "avc_crew_f" -> R.drawable.avatar_crew_f
        "avc_farmer_m" -> R.drawable.avatar_farmer_m
        "avc_lolo" -> R.drawable.avatar_lolo_m
        "avc_igorot_m" -> R.drawable.avatar_igorot_m
        "avc_igorot_f" -> R.drawable.avatar_igorot_f
        "avc_maranao_m" -> R.drawable.avatar_maranao_m
        "avc_maranao_f" -> R.drawable.avatar_maranao_f
        "avc_dahlia" -> R.drawable.avatar_dahlia
        "avc_aly" -> R.drawable.avatar_aly
        "avc_mau" -> R.drawable.avatar_mau
        "avc_kyla" -> R.drawable.avatar_kyla
        "avc_yow" -> R.drawable.avatar_yow
        "avc_ian" -> R.drawable.avatar_ian
        "avc_belen" -> R.drawable.avatar_belen
        else -> null
    }

    @DrawableRes
    fun documentary(id: String?): Int? = when (id) {
        "doc_adobo" -> R.drawable.reel_adobo
        "doc_sinigang" -> R.drawable.reel_sinigang
        "doc_paksiw" -> R.drawable.reel_paksiw
        "doc_sisig" -> R.drawable.reel_sisig
        "doc_mechado" -> R.drawable.reel_mechado
        "doc_menudo" -> R.drawable.reel_menudo
        "doc_caldereta" -> R.drawable.reel_caldereta
        "doc_afritada" -> R.drawable.reel_afritada
        "doc_humba" -> R.drawable.reel_humba
        "doc_pinikpikan" -> R.drawable.reel_pinikpikan
        "doc_inabraw" -> R.drawable.reel_inabraw
        "doc_pinuneg" -> R.drawable.reel_pinuneg
        "doc_sinursur" -> R.drawable.reel_sinursur
        "doc_binakol" -> R.drawable.reel_binakol
        "doc_la_paz_batchoy" -> R.drawable.reel_la_paz_batchoy
        "doc_inasal" -> R.drawable.reel_inasal
        "doc_kansi" -> R.drawable.reel_kansi
        "doc_piaya" -> R.drawable.reel_piaya
        "doc_tiyula_itum" -> R.drawable.reel_tiyula_itum
        "doc_piaparan" -> R.drawable.reel_piaparan
        "doc_pastil" -> R.drawable.reel_pastil
        "doc_sinuglaw" -> R.drawable.reel_sinuglaw
        "doc_kulma" -> R.drawable.reel_kulma
        "doc_satti" -> R.drawable.reel_satti
        "doc_pigar_pigar" -> R.drawable.reel_pigar_pigar
        "doc_bulalo" -> R.drawable.reel_bulalo
        "doc_chicharon_carcar" -> R.drawable.reel_chicharon_carcar
        "doc_lechon" -> R.drawable.reel_lechon
        "doc_bicol_express" -> R.drawable.reel_bicol_express
        "doc_laing" -> R.drawable.reel_laing
        "doc_palengke" -> R.drawable.reel_pastil
        "doc_mindanao" -> R.drawable.reel_tiyula_itum
        else -> null
    }

    @DrawableRes
    fun frame(id: String?): Int? = when (id) {
        "av_lei" -> R.drawable.frame_01
        "av_toque" -> R.drawable.frame_02
        "av_apron" -> R.drawable.frame_03
        "av_kawali" -> R.drawable.frame_04
        "av_salakot" -> R.drawable.frame_05
        "av_bougainvillea" -> R.drawable.frame_06
        "av_pearl" -> R.drawable.frame_07
        "av_tarsier" -> R.drawable.frame_08
        "av_sarimanok" -> R.drawable.frame_09
        "av_araw" -> R.drawable.frame_10
        "av_yakan" -> R.drawable.frame_11
        "av_butanding" -> R.drawable.frame_12
        "av_vinta" -> R.drawable.frame_13
        "av_jeepney" -> R.drawable.frame_14
        "av_parol" -> R.drawable.frame_15
        else -> null
    }
}
