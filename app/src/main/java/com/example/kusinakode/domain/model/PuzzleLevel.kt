package com.example.kusinakode.domain.model

data class PuzzleLevel(
    val number: Int,
    /** Uppercase answer; its length drives the grid size — never assume 6. */
    val answer: String,
    val displayName: String,
    val trivia: String,
    /** The dish photograph. */
    val imageRes: Int,
    /** The illustrated heritage card this dish awards on a win. */
    val cardRes: Int,
    val region: Region = Region.LUZON,
    /**
     * Set only for a level the admin panel added, whose art is on the
     * server rather than in the APK. When null the resources above are the
     * real thing; when set they are only a placeholder to draw meanwhile.
     */
    val imageUrl: String? = null,
    val cardUrl: String? = null
) {
    val wordLength: Int get() = answer.length
}
