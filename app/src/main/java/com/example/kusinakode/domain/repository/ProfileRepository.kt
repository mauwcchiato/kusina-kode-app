package com.example.kusinakode.domain.repository

import com.example.kusinakode.domain.model.ProfileStats
import com.example.kusinakode.domain.model.UserSession

interface ProfileRepository {
    suspend fun profileStats(userId: Int): Result<ProfileStats>

    /** Updates username/nickname and refreshes the local session on success. */
    suspend fun updateProfile(userId: Int, username: String, nickname: String?): Result<UserSession>
}
