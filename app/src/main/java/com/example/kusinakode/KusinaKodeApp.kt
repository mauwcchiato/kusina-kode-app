package com.example.kusinakode

import android.app.Application
import com.example.kusinakode.data.net.NetworkStatus
import com.example.kusinakode.data.repository.ChainRewardCoordinator
import com.example.kusinakode.data.repository.LocalGamificationRepository
import com.example.kusinakode.data.repository.RemoteGamificationSync
import com.example.kusinakode.domain.gamification.GamificationCoordinator
import com.example.kusinakode.domain.repository.GamificationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class KusinaKodeApp : Application() {

    /** Shared gamification store, exposed for ViewModels. */
    lateinit var gamification: GamificationRepository
        private set

    /** Exposed so screens can ask for a catch-up when they open. */
    lateinit var gamificationCoordinator: GamificationCoordinator
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // Before anything that can make a request, so the very first failure
        // is already classified correctly rather than guessed at.
        NetworkStatus.warm(this)

        gamification = LocalGamificationRepository(this)

        gamificationCoordinator = GamificationCoordinator(
            repository = gamification,
            currentUserId = { Session.userId },
            // Dishes cleared under the old build, so replaying them can't pay out.
            previouslySolved = {
                val highest = CompletedManager.getHighestCompleted(this, Session.userId)
                if (highest > 0) (1..highest).toSet() else emptySet()
            },
            sync = RemoteGamificationSync()
        )

        // Listens for the whole app lifetime, so points are credited no matter
        // which screen the player is on when a round ends.
        gamificationCoordinator.start(appScope)

        // Module 3's half of that seam: a badge announced by the gamification
        // layer is minted straight away rather than waiting for the player to
        // find a CLAIM button.
        KusinaNotifications.ensureChannels(this)
        PlayNudgeScheduler.sync(this)

        SoundFx.warm(this)

        ChainRewardCoordinator(
            context = this,
            onConfirmed = { gamificationCoordinator.refresh() }
        ).start(appScope)
    }
}
