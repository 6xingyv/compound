plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.androidlib)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    android {
        namespace = "com.mocharealm.tcsettings.compose"
        compileSdk = 36
        minSdk = 33

        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    sourceSets {
        androidMain.dependencies {
            api(project(":tcsettings:core"))
            implementation(libs.koin.androidx.compose)
            implementation("androidx.compose.runtime:runtime:1.11.0")
        }
    }
}
