package com.example.kusinakode.ui.levels

data class LevelsUiState(
    /** Highest unlocked level after merging server + local. */
    val unlockedUpTo: Int = 1,
    val isLoading: Boolean = true,
    val error: String? = null
)
