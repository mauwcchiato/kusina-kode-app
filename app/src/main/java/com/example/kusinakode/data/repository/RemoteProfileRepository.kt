package com.example.kusinakode.data.repository

import android.content.Context
import com.example.kusinakode.Session
import com.example.kusinakode.SessionStore
import com.example.kusinakode.api.KusinaApi
import com.example.kusinakode.domain.model.ProfileStats
import com.example.kusinakode.domain.model.UserSession
import com.example.kusinakode.domain.repository.ProfileRepository

class RemoteProfileRepository(context: Context) : ProfileRepository {

    private val appContext = context.applicationContext

    override suspend fun profileStats(userId: Int): Result<ProfileStats> = runCatching {
        val resp = KusinaApi.getProfileStats(userId)
        val data = resp.data
        if (resp.status == "success" && data != null) {
            ProfileStats(
                currentRank = data.current_rank,
                highestLevel = data.highest_level,
                bestTime = data.best_time,
                name = data.name,
                nickname = data.nickname,
                email = data.email
            )
        } else {
            throw IllegalStateException(resp.message ?: "Failed to load profile.")
        }
    }.mapNetworkError()

    override suspend fun updateProfile(
        userId: Int,
        username: String,
        nickname: String?
    ): Result<UserSession> = runCatching {
        val resp = KusinaApi.updateProfile(userId, username.trim(), nickname?.trim()?.ifBlank { null })
        if (resp.status == "success" && resp.display_name != null) {
            Session.displayName = resp.display_name
            Session.nickname = resp.nickname
            Session.email = resp.email ?: Session.email
            SessionStore.save(appContext)
            UserSession(
                userId = resp.user_id ?: userId,
                displayName = resp.display_name,
                nickname = resp.nickname,
                email = resp.email ?: Session.email ?: ""
            )
        } else {
            throw IllegalStateException(resp.message ?: "Failed to update profile")
        }
    }.mapNetworkError()
}
