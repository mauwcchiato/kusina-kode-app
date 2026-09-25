package com.example.kusinakode.data.net

import android.content.Context
import android.os.Build
import com.example.kusinakode.BuildConfig

/**
 * Where the REST API lives.
 *
 * Defect D-19: the laptop running XAMPP gets its address from DHCP, so
 * [DEFAULT_LAN_HOST] goes stale whenever it joins a different router. Baked
 * into a `const`, that meant every address change needed a code edit and a
 * rebuild before anyone could test on real hardware — and when it was missed
 * the app simply looked broken, because a wrong host fails the same way a
 * dead server does.
 *
 * So the host is resolved at runtime instead:
 *
 *  1. an override saved on the device, set from Settings, if there is one;
 *  2. otherwise [DEFAULT_LAN_HOST], the value compiled in.
 *
 * The emulator ignores both and uses 10.0.2.2, its private alias for the host
 * machine's loopback, which never changes.
 */
object ServerConfig {

    /**
     * Fallback when nothing is saved: the Azure VM running the API.
     *
     * This was the XAMPP laptop's LAN address, which is why the name says LAN
     * and why the override below exists at all. The backend now has a public,
     * static address, so the default no longer moves and a release build
     * reaches the cloud without anyone typing anything.
     *
     * It is a DNS name, not the bare IP (4.193.189.219), because the API is now
     * served over HTTPS and a certificate cannot be issued for a raw IP. The
     * VM's public IP carries this free Azure DNS label, and Let's Encrypt
     * issued a certificate for exactly this name (see [scheme]/[baseUrl]).
     */
    const val DEFAULT_LAN_HOST = "kusinakode.southeastasia.cloudapp.azure.com"

    /** The emulator's alias for the development machine. Not configurable. */
    private const val EMULATOR_HOST = "10.0.2.2"

    private const val PREFS = "kusinakode_prefs"
    private const val KEY_HOST = "server_host_override"

    private const val PATH = "/kusinakode/REST/"

    /**
     * 10.0.2.2 routes nowhere on a real phone, so one build has to tell the
     * two apart at runtime rather than rely on a flag someone remembers to
     * flip before testing on hardware.
     */
    val isEmulator: Boolean =
        Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.lowercase().contains("vbox") ||
            Build.FINGERPRINT.lowercase().contains("emulator") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for") ||
            Build.PRODUCT.startsWith("sdk") ||
            Build.PRODUCT.contains("sdk_gphone") ||
            Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu")

    @Volatile
    private var override: String? = null

    /**
     * Reads the saved override. Call once at startup, before any API call.
     *
     * The override only exists to let QA repoint a debug build at a local
     * XAMPP or a different laptop. It is set from a Settings section that is
     * itself shown only on debug builds (BuildConfig.DEBUG). A release build
     * must therefore ignore any saved value: with no UI left to clear it, a
     * stale LAN address left on the device would send a shipped app somewhere
     * the backend is not — and a value planted by someone with local/ADB
     * access could quietly redirect every request. Release always talks to the
     * compiled DEFAULT_LAN_HOST (the Azure API), so connectivity is unchanged.
     */
    fun load(context: Context) {
        override = if (BuildConfig.DEBUG) {
            prefs(context).getString(KEY_HOST, null)?.trim()?.takeIf { it.isNotEmpty() }
        } else {
            null
        }
    }

    /**
     * The host the app will talk to: an IP or hostname, no scheme or path.
     *
     * An override wins everywhere, emulator included, and with none set both
     * the emulator and a real device go to [DEFAULT_LAN_HOST].
     *
     * The emulator used to be forced to [EMULATOR_HOST] and to ignore any
     * override, because the only backend was the developer's own laptop and
     * 10.0.2.2 was the sole route to it. The API now has a public address the
     * emulator can reach directly, so that special case would only send the
     * emulator somewhere the rest of the team is not looking. To develop
     * against a local XAMPP again, set the override to 10.0.2.2 in Settings -
     * which is why it is honoured on the emulator now.
     */
    val host: String
        get() = override ?: DEFAULT_LAN_HOST

    /**
     * http or https, chosen by whether an override is in effect.
     *
     * The production host ([DEFAULT_LAN_HOST]) is reached over HTTPS: its
     * certificate is what keeps the bearer token and the login password off the
     * wire in the clear. A saved override, on the other hand, only ever exists
     * on a debug build (see [load]) and points at a local XAMPP / LAN laptop,
     * which has no certificate - so an override implies plain http.
     *
     * Net effect: release is always https (override is null there); debug is
     * https against production and http against a local box. This is the same
     * split the network security config enforces, kept in one place so the URL
     * and the platform's cleartext policy cannot drift apart.
     */
    val scheme: String
        get() = if (override != null) "http" else "https"

    /** Full base, ending in a slash, so call sites stay `"${BASE}login.php"`. */
    val baseUrl: String
        get() = "$scheme://$host$PATH"

    /** What Settings shows in the field: the override only, not the default. */
    fun savedOverride(): String? = override

    /**
     * Saves an override. Accepts what someone is likely to paste — a bare IP,
     * or a full URL — and keeps just the host, since the scheme and the REST
     * path are ours to decide.
     */
    fun setHost(context: Context, raw: String) {
        val cleaned = raw.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore('/')
            .trim()
        if (cleaned.isEmpty()) { clear(context); return }
        override = cleaned
        prefs(context).edit().putString(KEY_HOST, cleaned).apply()
    }

    /** Forgets the override and falls back to the compiled default. */
    fun clear(context: Context) {
        override = null
        prefs(context).edit().remove(KEY_HOST).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
