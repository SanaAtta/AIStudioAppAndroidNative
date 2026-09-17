plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

import java.util.Properties

fun String.escapeForBuildConfig(): String =
    replace("\\", "\\\\").replace("\"", "\\\"")

fun loadLocalApiKey(vararg keys: String): String {
    val secrets = rootProject.file("secrets.properties")
    if (secrets.exists()) {
        val props = Properties()
        secrets.inputStream().use { props.load(it)
        }
        for (key in keys) {
            props.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        }
    }
    val jsonFile = rootProject.file("firebase/api_config.json")
    if (jsonFile.exists()) {
        val text = jsonFile.readText()
        for (key in keys) {
            val regex = """"$key"\s*:\s*"([^"]*)"""".toRegex()
            regex.find(text)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        }
    }
    return ""
}

android {
    val localProps = Properties().apply {
        rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
    }
    fun escQuotes(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")
    val defaultAdmobAppId = "ca-app-pub-7377715706921836~3455916851"

    namespace = "com.aiartgenerator.imagegenerator.videogenerator"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.aiartgenerator.imagegenerator.videogenerator"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "FALLBACK_POLLINATIONS_API_KEY",
            "\"${loadLocalApiKey("pollinations_api_key", "pollinations.api.key").escapeForBuildConfig()}\"",
        )
        buildConfigField(
            "String",
            "FALLBACK_DEAPI_API_KEY",
            "\"${loadLocalApiKey("deapi_api_key", "deapi.api.key").escapeForBuildConfig()}\"",
        )
        buildConfigField(
            "String",
            "FALLBACK_GROQ_API_KEY",
            "\"${loadLocalApiKey("groq_api_key", "groq.api.key").escapeForBuildConfig()}\"",
        )
        buildConfigField(
            "String",
            "FALLBACK_GROK_API_KEY",
            "\"${loadLocalApiKey("grok_api_key", "grok.api.key", "xai_api_key", "xai.api.key").escapeForBuildConfig()}\"",
        )

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
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseAdmobAppId =
                localProps.getProperty("admob.app.id", "").trim().ifEmpty { defaultAdmobAppId }
            manifestPlaceholders["admobAppId"] = releaseAdmobAppId
            buildConfigField("String", "ADMOB_APP_ID", "\"${escQuotes(releaseAdmobAppId)}\"")
            buildConfigField("boolean", "USE_PRODUCTION_AD_UNITS", "true")
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
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.navigation.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.config)
    implementation(libs.firebase.messaging)
    implementation(libs.billing.ktx)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
    implementation(libs.gson)
    implementation(libs.sdp.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.appcompat)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
