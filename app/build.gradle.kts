import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Kotlin Serialization plugin
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23"
}

// Release signing, read from keystore.properties, which is gitignored.
//
// The debug key is per-machine: an APK built on one laptop cannot install
// over one built on another, so a tester has to uninstall first and loses
// their progress. A single shared release key is what makes an update an
// update. Absent the file the release build simply falls back to the debug
// key, so a fresh clone still builds.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "com.example.kusinakode"
    compileSdk = 34

    defaultConfig {
        // The install identity, and deliberately not com.example.*.
        //
        // Play Protect treats the Android Studio template package as a
        // signal in its own right — sideloading com.example.kusinakode
        // produced "App blocked to protect your device" on a clean phone
        // even though the build is properly signed and asks for nothing
        // beyond Internet, notifications and vibrate.
        //
        // Only applicationId changes. `namespace` below stays as it was, so
        // the R class, every Kotlin package and every import are untouched:
        // the two are independent, and moving the source would be a large
        // diff for no benefit.
        applicationId = "ph.kusinakode.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 13
        versionName = "1.12"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // The real release key when keystore.properties is present, and
            // the debug key otherwise so a fresh clone still builds. Anything
            // handed to another person should come from a release build: the
            // debug key differs per machine, so two people's builds cannot
            // update each other.
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")

            // R8 left off deliberately. The app leans on kotlinx.serialization
            // and Compose, both of which need keep rules to survive shrinking,
            // and a stripped class that only fails at runtime is the last thing
            // to discover during a demo. Size is not a constraint here.
            isMinifyEnabled = false

            // What this build type is really for: BuildConfig.DEBUG turns false,
            // which hides the SERVER (DEBUG) section in Settings. That section
            // exists so QA can repoint the host on real hardware, and a player
            // should never see it.
            //
            // Note the host override is still read from SharedPreferences on a
            // release build. A device that had one saved keeps using it with no
            // UI left to clear it, so clear app data when handing a phone over.
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    // --- Compose UI ---
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui-text")

    // --- Coroutines ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // --- Ktor Client & Serialization ---
    implementation("io.ktor:ktor-client-core:2.3.1")
    implementation("io.ktor:ktor-client-cio:2.3.1")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

    // --- Encrypted storage for the session token ---
    implementation("androidx.security:security-crypto:1.0.0")

    // --- Notifications (NotificationCompat) ---
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Level art for dishes added from the admin panel: those have no compiled
    // drawable, only a URL on the XAMPP host. Coil also caches to disk, so a
    // level seen once still renders offline.
    implementation("io.coil-kt:coil-compose:2.6.0")

    // --- Google Sign-In (Credential Manager) ---
    // Credential Manager, not the old Google Sign-In SDK: that one is
    // deprecated, and this is the API Google now supports on Android 14+.
    // credentials-play-services-auth is the piece that actually talks to
    // Play Services, so both are needed - the base artifact alone compiles
    // and then finds no provider at runtime.
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // --- Unit testing ---
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // --- Instrumented UI testing (Compose) ---
    // Needed by GameNavigationStressTest (androidTest). ui-test-manifest is a
    // debugImplementation so the empty test activity ships only in debug builds.
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.05.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
