import com.android.utils.usLocaleDecapitalize
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

fun getSecretProperty(key: String): String? {
    return localProperties.getProperty(key) ?: project.findProperty(key) as? String
}

android {
    namespace = "com.mocharealm.compound"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.mocharealm.compound"
        minSdk = 33
        targetSdk = 36
        versionCode = libs.versions.appVersionCode.get().toInt()
        versionName = libs.versions.appVersionName.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val apiId = getSecretProperty("API_ID") ?: "0"
        val apiHash = getSecretProperty("API_HASH") ?: ""

        buildConfigField("int", "TD_API_ID", apiId)
        buildConfigField("String", "TD_API_HASH", "\"$apiHash\"")
    }

    sourceSets {
        getByName("main") {
            jniLibs.directories.addAll(listOf("jniLibs"))
        }
    }

    signingConfigs {
        val sFile = getSecretProperty("RELEASE_STORE_FILE")
        val sPassword = getSecretProperty("RELEASE_STORE_PASSWORD")
        val kAlias = getSecretProperty("RELEASE_KEY_ALIAS")
        val kPassword = getSecretProperty("RELEASE_KEY_PASSWORD")

        if (sFile != null && sPassword != null && kAlias != null && kPassword != null) {
            create("release") {
                storeFile = file(sFile)
                storePassword = sPassword
                keyAlias = kAlias
                keyPassword = kPassword

                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
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
    splits {
        abi {
            isEnable = true
            isUniversalApk = false
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86_64", "x86")
        }
    }
}

base {
    archivesName.set(
        "${rootProject.name.usLocaleDecapitalize()}-${libs.versions.appVersionName.get().replace(" ", "-")}"
    )
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.compose.navigation3)

    implementation(libs.bundles.miuix.android)
    implementation(libs.androidx.nav3.runtime)
    implementation(libs.bundles.miuix.nav3)
    implementation(libs.gaze.capsule)
    implementation(libs.gaze.glassy.liquid.effect)

    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.lottie.compose)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    implementation(libs.kotlinx.serialization.core)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}