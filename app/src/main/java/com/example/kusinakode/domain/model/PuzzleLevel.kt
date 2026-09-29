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
    val cardUrl: String? = null,
    /**
     * Columns (0-based) followed by a "-" on the board, for a dish whose name
     * is hyphenated: Pigar-Pigar shows PIGAR-PIGAR. Display only - the answer
     * stays letters, so typing, hints and scoring are unchanged.
     */
    val hyphenAfter: Set<Int> = emptySet()
) {
    val wordLength: Int get() = answer.length

    companion object {
        /**
         * Where [name]'s hyphens fall among [answer]'s letters, or none when the
         * name's letters are not exactly the answer (then the hyphen belongs
         * to a word the puzzle does not use).
         */
        fun hyphensOf(name: String, answer: String): Set<Int> {
            if (name.filter { it.isLetter() }.uppercase() != answer) return emptySet()
            val out = HashSet<Int>()
            var letters = 0
            for (ch in name) {
                if (ch.isLetter()) letters++
                else if (ch == '-' && letters in 1 until answer.length) out += letters - 1
            }
            return out
        }
    }
}
