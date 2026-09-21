package com.example.kusinakode.data.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Whether the device has a working internet connection, asked of Android
 * rather than guessed from whatever the socket threw.
 *
 * The guess used to be the exception type: UnknownHostException meant
 * offline, a refused connection or a timeout meant the server was down. That
 * works only while the API is addressed by name. Ours is a bare IP
 * (ServerConfig.DEFAULT_LAN_HOST), and connecting to a literal address
 * performs no DNS lookup at all - so UnknownHostException is never thrown,
 * the offline branch was unreachable in practice, and a player with Wi-Fi off
 * was told the server had moved.
 *
 * Holds the application Context - never an Activity - so the error mapper can
 * consult it without a Context being threaded through every repository.
 */
object NetworkStatus {

    @Volatile
    private var appContext: Context? = null

    /** Called once from KusinaKodeApp.onCreate, like SoundFx.warm. */
    fun warm(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * True when Android believes this device can reach the internet.
     *
     * Requires NET_CAPABILITY_VALIDATED as well as NET_CAPABILITY_INTERNET.
     * The first says the link is up; only the second says something at the
     * other end actually answered. Campus and cafe Wi-Fi that wants a login
     * first is INTERNET but not VALIDATED, and calling that "online" would
     * put us straight back to blaming the server for a captive portal.
     *
     * Returns true when it cannot tell. An unknown state must not turn a real
     * server error into "you're offline" - being wrong in that direction
     * sends someone to check a router that is working fine.
     */
    fun isOnline(): Boolean {
        val cm = appContext?.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return true

        val caps = runCatching {
            cm.getNetworkCapabilities(cm.activeNetwork)
        }.getOrNull() ?: return false

        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
