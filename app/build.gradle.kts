plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Kotlin Serialization plugin
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23"
}

android {
    namespace = "com.example.kusinakode"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.kusinakode"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            // Signed with the debug key so the release APK can actually be
            // installed for testing and the defence. That is fine for a build
            // handed round on a cable and NOT fine for Play Store distribution,
            // which needs a real upload key - see the release notes before
            // publishing anywhere.
            signingConfig = signingConfigs.getByName("debug")

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

    // Level art for dishes added from the admin panel: those have no compiled
    // drawable, only a URL on the XAMPP host. Coil also caches to disk, so a
    // level seen once still renders offline.
    implementation("io.coil-kt:coil-compose:2.6.0")

    // --- Unit testing ---
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
