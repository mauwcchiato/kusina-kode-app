package com.example.kusinakode.data.repository

import android.content.Context
import android.util.Log
import com.example.kusinakode.KusinaNotifications
import com.example.kusinakode.SoundFx
import com.example.kusinakode.api.KusinaApi
import com.example.kusinakode.domain.ChainActivity
import com.example.kusinakode.domain.ChainEvents
import com.example.kusinakode.domain.ChainQueue
import com.example.kusinakode.domain.GamificationEvents
import com.example.kusinakode.domain.model.Badge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Fires the reward pipeline the moment a badge is earned.
 *
 * The panel's own wording is that a badge is minted "automatically" on
 * victory. Before this existed a badge sat at pending until the player
 * happened to find the CLAIM button on the Rewards screen — which read as a
 * broken blockchain rather than a waiting action. Islands stay on the
 * Rewards CLAIM button (same as the daily) so Complete Philippines can
 * show CLAIMED and land in the inbox.
 *
 * [GamificationEvents.badgesEarned] is the seam the gamification module
 * already announced badges on for exactly this purpose. Awards are pushed to
 * the server before they are announced, so the badge is claimable by the time
 * this runs.
 *
 * Every step is best-effort. A failure leaves the badge pending and the next
 * sync retries it — a round is never blocked by the chain being unreachable.
 */
class ChainRewardCoordinator(
    private val context: Context,
    private val onConfirmed: suspend () -> Unit
) {

    fun start(scope: CoroutineScope) {
        scope.launch {
            GamificationEvents.badgesEarned.collect { badge ->
                mint(badge)
            }
        }
    }

    private suspend fun mint(badge: Badge) {
        val label = badge.title.ifBlank { "your achievement" }
        ChainEvents.report(ChainActivity.Validating("Validating $label on-chain..."))

        runCatching {
            // Queued: winning a dish can unlock two badges at once, and the
            // round's own reward is already in flight. See ChainQueue / D-31.
            ChainQueue.serialized {
                val claim = KusinaApi.claimKk(kind = "badge", badgeId = badge.badgeId)
                val txRef = claim.tx_ref
                if (claim.status != "success" || txRef == null) {
                    error(claim.message ?: "Reward could not be queued")
                }
                // Walks phases 2-4; phase 4 writes badges.tx_hash server-side.
                var settled = KusinaApi.settleReward(txRef)
                // One settle is three polls, which is exactly enough when every
                // phase succeeds. Give a slow chain a couple more rounds rather
                // than leaving the player watching a spinner forever.
                var tries = 0
                while (settled?.tx_hash.isNullOrBlank() &&
                    settled?.status != "failed" &&
                    tries < 2
                ) {
                    delay(1200)
                    settled = KusinaApi.settleReward(txRef)
                    tries++
                }
                settled
            }
        }.onSuccess { settled ->
            val hash = settled?.tx_hash
            if (hash.isNullOrBlank()) {
                // Still not confirmed after the extra polls. Reported as a
                // resting state, not a spinner: Validating never clears itself,
                // and a badge stuck mid-pipeline would hang the banner forever.
                ChainEvents.report(
                    ChainActivity.Failed("$label is taking longer than usual - it will retry.")
                )
            } else {
                val badgeName = badge.title.ifBlank { "Badge" }
                ChainEvents.report(
                    ChainActivity.Minted(
                        label = badgeName,
                        txHash = hash,
                        amountKk = settled.amount_kk
                    )
                )
                KusinaNotifications.badgeMinted(context, badgeName, settled.amount_kk)
                SoundFx.play(context, SoundFx.Cue.Badge)
            }
            // Pull the confirmed record down so the badge shelf flips from
            // pending to verified without waiting for a restart (D-28).
            runCatching { onConfirmed() }
        }.onFailure { e ->
            Log.w(TAG, "auto-mint failed for ${badge.badgeId}: ${e.localizedMessage}")
            ChainEvents.report(
                ChainActivity.Failed("Could not verify $label yet - it will retry.")
            )
        }
    }

    private companion object {
        const val TAG = "ChainRewards"
    }
}
