// File: app/src/main/java/com/example/kusinakode/api/KusinaApi.kt
@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.example.kusinakode.api

import com.example.kusinakode.data.net.ServerConfig
import com.kusinakode.KtorClient
import io.ktor.client.call.*
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

// --- Data classes ---
@Serializable
data class RegisterRequest(
    val name: String,
    val nickname: String? = null,
    val email: String,
    val password: String
)

@Serializable
data class RegisterResponse(
    val status: String,
    val user_id: Int? = null,
    /** Signing up signs you in, so the token arrives here too. */
    val token: String? = null,
    val message: String? = null
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(
    val status: String,
    val user_id: Int? = null,
    /** Bearer token for every account-scoped call that follows. */
    val token: String? = null,
    val display_name: String? = null,
    val nickname: String? = null,
    val email: String? = null,
    val message: String? = null
)

@Serializable
data class GenericListResponse<T>(val status: String, val data: List<T>? = null, val message: String? = null)

@Serializable
data class LevelData(
    val id: Int = 0,
    val word: String = "",
    /** Any of these can be null on a level added through the admin form. */
    val trivia: String? = null,
    val history_image: String? = null,
    val name: String? = null,
    val region: String? = null,
    val story: String? = null,
    val status: String? = null,
    val ingredients: String? = null,
    val steps: String? = null,
    val equipment: String? = null,
    val difficulty: String? = null,
    /** Sent as a quoted number by PDO, so read it as text and parse. */
    val cook_time_minutes: String? = null,
    val rating: String? = null,
    /** Relative to the XAMPP site root when it starts with "media/". */
    val image_path: String? = null
)

@Serializable
data class EquipmentData(
    val id: Int = 0,
    val name: String = "",
    val category: String? = null,
    val region: String? = null,
    /** Relative to the XAMPP site root when it starts with "media/". */
    val image_path: String? = null
)

@Serializable
data class UnlockRequest(val user_id: Int?, val level_id: Int)

@Serializable
data class UnlockResponse(
    val status: String,
    val message: String? = null,
    val tx_ref: String? = null,
    val tx_refs: List<String> = emptyList()
)

@Serializable
data class AttemptRequest(
    val user_id: Int?,
    val level_id: Int,
    val word: String,
    val correct: Boolean,
    /** Milliseconds, per the shared Attempt contract. */
    val time_taken_ms: Long
)

@Serializable
data class AttemptResponse(
    val status: String,
    val message: String? = null,
    val tx_ref: String? = null
)

@Serializable
data class LeaderboardEntry(
    val name: String,
    val correct_count: Int,
    /** Module 2 ranks by accumulated points; 0 for players yet to sync. */
    val total_points: Int = 0,
    val frame_id: String? = null,
    val character_id: String? = null
)

@Serializable
data class ProgressRequest(
    val user_id: Int,
    val total_points: Int,
    val rounds_completed: Int,
    val current_streak: Int,
    val best_streak: Int,
    val perfect_rounds: Int
)

@Serializable
data class BadgeRequest(
    val user_id: Int,
    val badge_id: String,
    val badge_type: String,
    val title: String,
    val milestone_criteria: String
)

@Serializable
data class ProgressData(
    val total_points: Int = 0,
    val rounds_completed: Int = 0,
    val current_streak: Int = 0,
    val best_streak: Int = 0,
    val perfect_rounds: Int = 0,
    val solved_levels: List<Int> = emptyList()
)

@Serializable
data class ProgressResponse(
    val status: String,
    /** Null when the account has no server-side record yet. */
    val data: ProgressData? = null,
    val message: String? = null
)

@Serializable
data class BadgeData(
    val badge_id: String,
    val user_id: Int? = null,
    val badge_type: String = "ROUNDS",
    val title: String = "",
    val milestone_criteria: String = "",
    val tx_hash: String? = null,
    val awarded_at: Long = 0
)

@Serializable
data class BadgesResponse(
    val status: String,
    val data: List<BadgeData>? = null,
    val message: String? = null
)

@Serializable
data class AttemptHistoryEntry(
    val level_id: Int,
    val level_name: String,
    val guess_word: String,
    val is_correct: Int,
    val time_taken_ms: Long? = null,
    val attempted_at: Long? = null
)

@Serializable
data class AttemptHistoryResponse(
    val status: String,
    val data: List<AttemptHistoryEntry>? = null,
    val message: String? = null
)

@Serializable
data class LeaderboardResponse(val status: String, val data: List<LeaderboardEntry>? = null, val message: String? = null)

@kotlinx.serialization.Serializable
data class ProfileStatsResponse(
    val status: String,
    val data: ProfileStatsData? = null,
    val message: String? = null
)

@kotlinx.serialization.Serializable
data class ProfileStatsData(
    val current_rank: Int? = null,
    val highest_level: Int,
    val best_time: String? = null,
    val name: String,
    val nickname: String? = null,
    val email: String
)

@Serializable
data class UpdateProfileRequest(
    val user_id: Int,
    val name: String,
    val nickname: String? = null
)

@Serializable
data class UpdateProfileResponse(
    val status: String,
    val user_id: Int? = null,
    val display_name: String? = null,
    val nickname: String? = null,
    val email: String? = null,
    val message: String? = null
)

@Serializable
data class RequestPasswordResetRequest(val email: String)

@Serializable
data class RequestPasswordResetResponse(
    val status: String,
    val message: String? = null,
    val debug_code: String? = null
)

@Serializable
data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val new_password: String
)

@Serializable
data class ResetPasswordResponse(val status: String, val message: String? = null)

@Serializable
data class WalletBalanceData(
    val user_id: Int? = null,
    val wallet_address: String? = null,
    val balance_kk: Long = 0,
    val is_live: Boolean = false
)

@Serializable
data class WalletBalanceResponse(
    val status: String,
    val data: WalletBalanceData? = null,
    val message: String? = null
)

@Serializable
data class RewardStatusData(
    val tx_ref: String? = null,
    val phase: Int = 0,
    val status: String? = null,
    val tx_hash: String? = null,
    val amount_kk: Long? = null
)

@Serializable
data class RewardStatusResponse(
    val status: String,
    val reward: RewardStatusData? = null,
    val message: String? = null
)

@Serializable
data class SpendKkRequest(val power_up: String)

@Serializable
data class EquipLookRequest(
    val character_id: String = "",
    val frame_id: String = ""
)

@Serializable
data class SpendKkResponse(
    val status: String,
    val tx_ref: String? = null,
    val message: String? = null
)

@Serializable
data class EarnDailyData(
    val claimable: Boolean = false,
    val claimed: Boolean = false,
    val amount_kk: Long = 5,
    val claim_key: String? = null,
    val timezone: String? = null
)

@Serializable
data class EarnIslandData(
    val id: String,
    val name: String,
    val solved: Int = 0,
    val total: Int = 0,
    val claimed: Boolean = false,
    val claimable: Boolean = false,
    val amount_kk: Long = 15
)

@Serializable
data class EarnStatusData(
    val daily: EarnDailyData? = null,
    val islands: List<EarnIslandData> = emptyList(),
    val badges: List<EarnBadgeData> = emptyList(),
    val round_win_kk: Long = 10,
    val badge_kk: Long = 25
)

@Serializable
data class EarnBadgeData(
    val id: String,
    val title: String = "",
    val claimed: Boolean = false,
    val claimable: Boolean = false,
    val amount_kk: Long = 25
)

@Serializable
data class EarnStatusResponse(
    val status: String,
    val data: EarnStatusData? = null,
    val message: String? = null
)

@Serializable
data class ClaimKkRequest(val kind: String, val island: String? = null, val badge_id: String? = null)

@Serializable
data class ClaimKkResponse(
    val status: String,
    val tx_ref: String? = null,
    val message: String? = null
)

fun ClaimKkResponse.alreadyClaimed(): Boolean =
    status.equals("already", ignoreCase = true) ||
        message?.contains("already claimed", ignoreCase = true) == true

@Serializable
data class RewardHistoryItem(
    val tx_ref: String? = null,
    val event_type: String? = null,
    val amount_kk: Long? = null,
    val tx_hash: String? = null,
    val status: String? = null,
    val phase: Int? = null,
    val title: String? = null,
    val created_at: String? = null
)

@Serializable
data class RewardHistoryResponse(
    val status: String,
    val data: List<RewardHistoryItem> = emptyList(),
    val message: String? = null
)

@Serializable
data class ShopOwnedData(
    val owned: List<String> = emptyList(),
    val equipped: Map<String, String> = emptyMap()
)

@Serializable
data class ShopOwnedResponse(
    val status: String,
    val data: ShopOwnedData? = null,
    val message: String? = null
)

@Serializable
data class PantryJarDto(
    val id: String,
    val qty: Int = 0,
    val found_in_level: Int? = null
)

@Serializable
data class PantrySnapshotData(
    val jars: List<PantryJarDto> = emptyList(),
    val draws_available: Int = 0,
    val spins_available: Int = 0,
    val collected: Int = 0,
    val total: Int = 0,
    val jar_count: Int = 0,
    val sell_value_kk: Int = 0,
    val balance_kk: Long = 0
)

@Serializable
data class PantrySnapshotResponse(
    val status: String,
    val data: PantrySnapshotData? = null,
    val message: String? = null
)

/** What one wheel spin paid out. The server rolls it; the client only reads. */
@Serializable
data class PantrySpinData(
    val palayoks_won: Int = 0,
    val snapshot: PantrySnapshotData? = null
)

@Serializable
data class PantrySpinResponse(
    val status: String,
    val data: PantrySpinData? = null,
    val message: String? = null
)

@Serializable
data class PantryGrantRequest(val level_id: Int, val power_ups_used: Int = 0)

@Serializable
data class PantryGrantResponse(
    val status: String,
    val granted: Int = 0,
    val already_claimed: Boolean = false,
    val message: String? = null
)

@Serializable
data class PantryDrawRequest(val level_id: Int? = null)

@Serializable
data class PantryDrawData(
    val ingredient_id: String? = null,
    val is_duplicate: Boolean = false,
    val qty: Int = 0,
    val draws_left: Int = 0,
    val collected: Int = 0,
    val snapshot: PantrySnapshotData? = null,
    val balance_kk: Long = 0
)

@Serializable
data class PantryDrawResponse(
    val status: String,
    val data: PantryDrawData? = null,
    val message: String? = null
)

@Serializable
data class PantrySellRequest(val ingredient_id: String, val qty: Int = 1)

@Serializable
data class PantrySellData(
    val ingredient_id: String,
    val sold: Int = 0,
    val kk_awarded: Int = 0,
    val qty_left: Int = 0,
    val snapshot: PantrySnapshotData? = null,
    val balance_kk: Long = 0,
    val tx_ref: String? = null
)

@Serializable
data class PantrySellResponse(
    val status: String,
    val data: PantrySellData? = null,
    val message: String? = null
)

/** Reason keys the server accepts - see KK_REPORT_REASONS in lib/reports.php. */
object ReportReason {
    const val WRONG_AMOUNT = "wrong_amount"
    const val NOT_RECEIVED = "not_received"
    const val OTHER = "other"
}

@Serializable
data class ReportSubmitRequest(
    val tx_ref: String,
    val reason: String,
    val message: String? = null
)

@Serializable
data class ReportSubmitData(val id: Int)

@Serializable
data class ReportSubmitResponse(
    val status: String,
    val data: ReportSubmitData? = null,
    val message: String? = null
)

// --- API wrapper ---
object KusinaApi {
    /**
     * Resolved at runtime by [ServerConfig] rather than compiled in: the
     * laptop's address comes from DHCP (defect D-19), so it can be corrected
     * from Settings on the device instead of needing a code edit and a new
     * build every time the network changes.
     */
    private val BASE: String get() = ServerConfig.baseUrl

    suspend fun register(
        name: String,
        nickname: String?,
        email: String,
        password: String
    ): RegisterResponse =
        KtorClient.client.post("${BASE}register.php") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(name, nickname, email, password))
        }.body()

    suspend fun login(email: String, password: String): LoginResponse =
        KtorClient.client.post("${BASE}login.php") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }.body()

    suspend fun getLevels(): GenericListResponse<LevelData> =
        KtorClient.client.get("${BASE}get_levels.php").body()

    /** Tool name to uploaded picture, so a card can show the panel's art. */
    suspend fun getEquipment(): List<EquipmentData> =
        KtorClient.client.get("${BASE}get_equipment.php")
            .body<GenericListResponse<EquipmentData>>().data.orEmpty()

    suspend fun postUnlock(userId: Int?, levelId: Int): UnlockResponse =
        KtorClient.client.post("${BASE}post_unlock.php") {
            contentType(ContentType.Application.Json)
            setBody(UnlockRequest(userId, levelId))
        }.body()

    suspend fun getUnlocks(userId: Int?): GenericListResponse<Int> =
        KtorClient.client.get("${BASE}get_unlocks.php?user_id=$userId").body()

    suspend fun postAttempt(
        userId: Int,
        levelId: Int,
        guess: String,
        correct: Boolean,
        timeTakenMs: Long
    ): AttemptResponse = KtorClient.client.post("${BASE}post_attempt.php") {
        contentType(ContentType.Application.Json)
        setBody(AttemptRequest(userId, levelId, guess, correct, timeTakenMs))
    }.body()


    /** [window] is all, today or week — see get_leaderboard.php. */
    suspend fun getLeaderboard(limit: Int = 10, window: String = "all"): LeaderboardResponse =
        KtorClient.client.get("${BASE}get_leaderboard.php?limit=$limit&window=$window").body()

    suspend fun postProgress(request: ProgressRequest): UnlockResponse =
        KtorClient.client.post("${BASE}post_progress.php") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun postBadge(request: BadgeRequest): UnlockResponse =
        KtorClient.client.post("${BASE}post_badge.php") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun getAttemptHistory(userId: Int, limit: Int = 30): AttemptHistoryResponse =
        KtorClient.client.get("${BASE}get_attempts.php?user_id=$userId&limit=$limit").body()

    suspend fun getProgress(userId: Int): ProgressResponse =
        KtorClient.client.get("${BASE}get_progress.php?user_id=$userId").body()

    suspend fun getBadges(userId: Int): BadgesResponse =
        KtorClient.client.get("${BASE}get_badges.php?user_id=$userId").body()

    suspend fun getProfileStats(userId: Int): ProfileStatsResponse =
        KtorClient.client.get("${BASE}get_profile_stats.php?user_id=$userId").body()

    suspend fun updateProfile(
        userId: Int,
        username: String,
        nickname: String?
    ): UpdateProfileResponse =
        KtorClient.client.post("${BASE}update_profile.php") {
            contentType(ContentType.Application.Json)
            setBody(UpdateProfileRequest(user_id = userId, name = username, nickname = nickname))
        }.body()

    suspend fun requestPasswordReset(email: String): RequestPasswordResetResponse =
        KtorClient.client.post("${BASE}request_password_reset.php") {
            contentType(ContentType.Application.Json)
            setBody(RequestPasswordResetRequest(email.trim()))
        }.body()

    suspend fun resetPassword(email: String, code: String, newPassword: String): ResetPasswordResponse =
        KtorClient.client.post("${BASE}reset_password.php") {
            contentType(ContentType.Application.Json)
            setBody(ResetPasswordRequest(email.trim(), code.trim(), newPassword))
        }.body()

    /**
     * Revokes the current token server-side.
     *
     * Clearing it on the device alone leaves it valid for its full lifetime,
     * so anyone who captured it could keep using it after the player signed
     * out. [allDevices] drops every session for the account.
     */
    suspend fun logout(allDevices: Boolean = false): UnlockResponse =
        KtorClient.client.post("${BASE}logout.php${if (allDevices) "?all=1" else ""}").body()

    suspend fun getWalletBalance(): WalletBalanceResponse =
        KtorClient.client.get("${BASE}wallet/balance.php").body()

    suspend fun getRewardStatus(txRef: String): RewardStatusResponse =
        KtorClient.client.get("${BASE}reward/status.php") {
            parameter("tx_ref", txRef)
        }.body()

    suspend fun spendKk(powerUp: String): SpendKkResponse {
        val http: io.ktor.client.statement.HttpResponse =
            KtorClient.client.post("${BASE}reward/spend.php") {
                expectSuccess = false
                contentType(ContentType.Application.Json)
                setBody(SpendKkRequest(powerUp))
            }
        val parsed = runCatching { http.body<SpendKkResponse>() }.getOrNull()
        if (http.status.value >= 400 || parsed?.status == "error") {
            error(parsed?.message ?: "Could not unlock")
        }
        return parsed ?: error("Could not unlock")
    }

    suspend fun equipLook(characterId: String?, frameId: String?): ShopOwnedResponse =
        KtorClient.client.post("${BASE}reward/equip.php") {
            contentType(ContentType.Application.Json)
            setBody(EquipLookRequest(characterId.orEmpty(), frameId.orEmpty()))
        }.body()

    suspend fun getEarnStatus(): EarnStatusResponse =
        KtorClient.client.get("${BASE}reward/earn.php").body()

    suspend fun claimKk(kind: String, island: String? = null, badgeId: String? = null): ClaimKkResponse {
        val http: io.ktor.client.statement.HttpResponse =
            KtorClient.client.post("${BASE}reward/claim.php") {
                expectSuccess = false
                contentType(ContentType.Application.Json)
                setBody(ClaimKkRequest(kind, island, badgeId))
            }
        val parsed = runCatching { http.body<ClaimKkResponse>() }.getOrNull()
        if (parsed?.alreadyClaimed() == true) {
            return parsed.copy(status = "already")
        }
        if (http.status.value >= 400 || parsed?.status == "error") {
            error(parsed?.message ?: "Could not claim")
        }
        return parsed ?: error("Could not claim")
    }

    suspend fun getRewardHistory(): RewardHistoryResponse =
        KtorClient.client.get("${BASE}reward/history.php").body()

    suspend fun getShopOwned(): ShopOwnedResponse =
        KtorClient.client.get("${BASE}reward/shop.php").body()

    /** Each call advances one phase. Three polls walk 2 → 3 → 4. */
    suspend fun settleReward(txRef: String): RewardStatusData? {
        var last: RewardStatusData? = null
        repeat(3) {
            last = getRewardStatus(txRef).reward
            if (last?.status == "confirmed" || last?.phase == 4) return last
        }
        return last
    }

    suspend fun getPantrySnapshot(): PantrySnapshotResponse {
        val http: io.ktor.client.statement.HttpResponse =
            KtorClient.client.get("${BASE}pantry/snapshot.php") {
                expectSuccess = false
            }
        val parsed = runCatching { http.body<PantrySnapshotResponse>() }.getOrNull()
        if (http.status.value >= 400 || parsed?.status == "error") {
            error(parsed?.message ?: "Could not load the pantry")
        }
        return parsed ?: error("Could not load the pantry")
    }

    suspend fun pantryGrant(levelId: Int, powerUpsUsed: Int = 0): Int {
        val http: io.ktor.client.statement.HttpResponse =
            KtorClient.client.post("${BASE}pantry/grant.php") {
                expectSuccess = false
                contentType(ContentType.Application.Json)
                setBody(PantryGrantRequest(levelId, powerUpsUsed))
            }
        val parsed = runCatching { http.body<PantryGrantResponse>() }.getOrNull()
        if (http.status.value >= 400 && parsed?.already_claimed != true) {
            error(parsed?.message ?: "Could not claim market runs")
        }
        return parsed?.granted ?: 0
    }

    /**
     * Buys one extra spin with KK. The price is fixed server-side, the same
     * way a power-up's is, so the client never names an amount.
     *
     * Only banks the spin — [pantrySpin] still rolls it, so the prize is
     * decided after the wheel is actually turned.
     */
    suspend fun pantryBuySpin(): PantrySnapshotData {
        val http: io.ktor.client.statement.HttpResponse =
            KtorClient.client.post("${BASE}pantry/buy_spin.php") {
                expectSuccess = false
                contentType(ContentType.Application.Json)
            }
        val parsed = runCatching { http.body<PantrySnapshotResponse>() }.getOrNull()
        if (http.status.value >= 400) {
            error(parsed?.message ?: "Could not buy a spin")
        }
        return parsed?.data ?: error("Could not buy a spin")
    }

    /** Spends one banked spin. The prize is decided server-side. */
    suspend fun pantrySpin(): PantrySpinData {
        val http: io.ktor.client.statement.HttpResponse =
            KtorClient.client.post("${BASE}pantry/spin.php") {
                expectSuccess = false
                contentType(ContentType.Application.Json)
            }
        val parsed = runCatching { http.body<PantrySpinResponse>() }.getOrNull()
        if (http.status.value >= 400) {
            error(parsed?.message ?: "Could not spin the wheel")
        }
        return parsed?.data ?: error("Could not spin the wheel")
    }

    suspend fun pantryDraw(levelId: Int?): PantryDrawResponse {
        val http: io.ktor.client.statement.HttpResponse =
            KtorClient.client.post("${BASE}pantry/draw.php") {
                expectSuccess = false
                contentType(ContentType.Application.Json)
                setBody(PantryDrawRequest(levelId))
            }
        val parsed = runCatching { http.body<PantryDrawResponse>() }.getOrNull()
        if (http.status.value >= 400 || parsed?.status == "error") {
            error(parsed?.message ?: "Could not open the palayok")
        }
        return parsed ?: error("Could not open the palayok")
    }

    suspend fun pantrySell(ingredientId: String, qty: Int): PantrySellResponse {
        val http: io.ktor.client.statement.HttpResponse =
            KtorClient.client.post("${BASE}pantry/sell.php") {
                expectSuccess = false
                contentType(ContentType.Application.Json)
                setBody(PantrySellRequest(ingredientId, qty))
            }
        val parsed = runCatching { http.body<PantrySellResponse>() }.getOrNull()
        if (http.status.value >= 400 || parsed?.status == "error") {
            error(parsed?.message ?: "Could not sell that ingredient")
        }
        return parsed ?: error("Could not sell that ingredient")
    }

    /**
     * Flags a specific receipt as wrong. [txRef] must belong to the caller -
     * the server checks that against the bearer token, never trusting the
     * client either way.
     */
    suspend fun submitReport(txRef: String, reason: String, message: String? = null): ReportSubmitResponse {
        val http: io.ktor.client.statement.HttpResponse =
            KtorClient.client.post("${BASE}report/submit.php") {
                expectSuccess = false
                contentType(ContentType.Application.Json)
                setBody(ReportSubmitRequest(txRef, reason, message))
            }
        val parsed = runCatching { http.body<ReportSubmitResponse>() }.getOrNull()
        if (http.status.value >= 400 || parsed?.status == "error") {
            error(parsed?.message ?: "Could not send that report")
        }
        return parsed ?: error("Could not send that report")
    }
}
