package com.example.kusinakode

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * System notifications for things that happen to a player's rewards.
 *
 * Kept deliberately small: two channels, no background scheduling. Rewards
 * announces a completed mint, Reminders nudges an unclaimed daily. Anything
 * that needs to fire while the app is closed would need WorkManager, which
 * this does not pull in.
 *
 * Every post is guarded — on Android 13+ notifications are a runtime
 * permission, and a denied permission must never crash a round.
 */
object KusinaNotifications {

    private const val CHANNEL_REWARDS = "kk_rewards"
    private const val CHANNEL_REMINDERS = "kk_reminders"

    private const val ID_MINTED = 1001
    private const val ID_DAILY = 1002
    private const val ID_ISLAND_READY = 1003
    private const val ID_ISLAND_CLAIMED = 1100

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REWARDS,
                "Rewards",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Badges minted on-chain and KK you have earned."
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDERS,
                "Reminders",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Daily claim and streak nudges."
            }
        )
    }

    /** True once the player has granted notification access (always true pre-13). */
    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    /** A badge finished the four-phase pipeline. */
    fun badgeMinted(context: Context, title: String, amountKk: Long?) {
        post(
            context,
            channel = CHANNEL_REWARDS,
            id = ID_MINTED,
            title = "Badge Successfully Minted",
            body = amountKk?.let { "+$it KK added to your wallet" } ?: title
        )
    }

    /** The daily claim is sitting unclaimed. */
    fun dailyClaimReady(context: Context, amountKk: Long) {
        post(
            context,
            channel = CHANNEL_REMINDERS,
            id = ID_DAILY,
            title = "Your daily +$amountKk KK is waiting",
            body = "Open Rewards to claim it before the day resets."
        )
    }

    /** An island set is complete and the +15 KK still needs a tap. */
    fun islandClaimReady(context: Context, islandName: String, amountKk: Long) {
        post(
            context,
            channel = CHANNEL_REMINDERS,
            id = ID_ISLAND_READY + islandName.lowercase().hashCode().and(0x3F),
            title = "Complete $islandName is waiting",
            body = "Open Rewards to claim +$amountKk KK."
        )
    }

    /** The island CLAIM just minted. */
    fun islandClaimed(context: Context, islandName: String, amountKk: Long) {
        post(
            context,
            channel = CHANNEL_REWARDS,
            id = ID_ISLAND_CLAIMED + islandName.lowercase().hashCode().and(0x3F),
            title = "$islandName complete",
            body = "+$amountKk KK added to your wallet."
        )
    }

    private fun post(
        context: Context,
        channel: String,
        id: Int,
        title: String,
        body: String
    ) {
        // Two gates: the player's own switch, then Android's permission.
        if (!KusinaSettings.prefs.value.notifications) return
        if (!canPost(context)) return
        ensureChannels(context)

        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            id,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // Permission can be revoked between the check above and the post.
        runCatching {
            NotificationManagerCompat.from(context).notify(id, notification)
        }
    }
}
