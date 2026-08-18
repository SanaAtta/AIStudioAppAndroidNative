import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

android {
    val localProps = Properties().apply {
        rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
    }
    fun escQuotes(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")
    val defaultAdmobAppId = "ca-app-pub-3940256099942544~3347511713"

    namespace = "com.example.myapplication"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val useProductionAdUnits =
            localProps.getProperty("admob.use.production.units", "false")
                .trim()
                .equals("true", ignoreCase = true)
        buildConfigField("boolean", "USE_PRODUCTION_AD_UNITS", useProductionAdUnits.toString())
        val umpTestDeviceHashedIds =
            escQuotes(localProps.getProperty("ump.test.device.hashed.id", "").trim())
        buildConfigField("String", "UMP_TEST_DEVICE_HASHED_IDS", "\"$umpTestDeviceHashedIds\"")
        val umpResetOnDebugLaunch =
            localProps.getProperty("ump.reset.on.debug.launch", "false")
                .trim()
                .equals("true", ignoreCase = true)
        buildConfigField("boolean", "UMP_RESET_ON_DEBUG_LAUNCH", umpResetOnDebugLaunch.toString())
    }

    buildTypes {
        debug {
            val debugAdmobAppId =
                localProps.getProperty("admob.app.id", "").trim().ifEmpty { defaultAdmobAppId }
            manifestPlaceholders["admobAppId"] = debugAdmobAppId
            buildConfigField("String", "ADMOB_APP_ID", "\"${escQuotes(debugAdmobAppId)}\"")
            buildConfigField("boolean", "USE_PRODUCTION_AD_UNITS", "false")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            val releaseAdmobAppId =
                localProps.getProperty("admob.app.id", "").trim().ifEmpty { defaultAdmobAppId }
            manifestPlaceholders["admobAppId"] = releaseAdmobAppId
            buildConfigField("String", "ADMOB_APP_ID", "\"${escQuotes(releaseAdmobAppId)}\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)
    implementation(libs.firebase.analytics)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
    implementation(libs.gson)
    implementation(libs.sdp.android)
    implementation(libs.kotlinx.coroutines.play.services)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
