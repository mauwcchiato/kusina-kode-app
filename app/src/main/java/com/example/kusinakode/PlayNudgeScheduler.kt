package com.example.kusinakode

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Quiet play reminders while the app is closed.
 *
 * Two lines, twelve hours apart: come plate a dish, or trade ingredients if
 * KK is short. Skipped if Reward notifications are off, or if the player
 * opened the app in the last few hours.
 */
object PlayNudgeScheduler {

    private const val WORK = "kk_play_nudge"
    const val PREFS = "kusinakode_prefs"
    const val LAST_OPEN = "nudge_last_open_ms"
    private const val QUIET_AFTER_OPEN_MS = 4L * 60 * 60 * 1000

    fun markOpened(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(LAST_OPEN, System.currentTimeMillis())
            .apply()
    }

    fun sync(context: Context) {
        KusinaSettings.load(context)
        if (KusinaSettings.prefs.value.notifications) schedule(context)
        else cancel(context)
    }

    fun schedule(context: Context) {
        val req = PeriodicWorkRequestBuilder<PlayNudgeWorker>(12, TimeUnit.HOURS)
            .setInitialDelay(12, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            req
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK)
    }

    fun shouldNudge(context: Context): Boolean {
        KusinaSettings.load(context)
        if (!KusinaSettings.prefs.value.notifications) return false
        val last = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(LAST_OPEN, 0L)
        return System.currentTimeMillis() - last >= QUIET_AFTER_OPEN_MS
    }
}

class PlayNudgeWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        if (!PlayNudgeScheduler.shouldNudge(applicationContext)) return Result.success()
        val play = (System.currentTimeMillis() / TimeUnit.HOURS.toMillis(12)) % 2L == 0L
        if (play) {
            KusinaNotifications.playNudge(applicationContext)
        } else {
            KusinaNotifications.tradeNudge(applicationContext)
        }
        return Result.success()
    }
}
