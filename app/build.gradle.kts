plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.folius.dotnotes"
    compileSdk = 36
    buildToolsVersion = "35.0.0"


    defaultConfig {
        applicationId = "com.folius.dotnotes"
        minSdk = 24
        targetSdk = 36
        versionCode = 12
        versionName = "0.3.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // CI: secrets RELEASE_KEYSTORE_BASE64 -> app/release.keystore decoded in workflow
            // Local: use real keystore only if password env/property is provided, else fallback debug
            val hasRealPass = System.getenv("RELEASE_KEYSTORE_PASSWORD") != null || project.findProperty("RELEASE_KEYSTORE_PASSWORD") != null
            val candidates = listOf(
                file("dotnotes-release.jks"),
                file("release.keystore"),
                file("${System.getProperty("user.home")}/.keystores/dotnotes-release.jks")
            )
            val real = candidates.firstOrNull { it.exists() }
            if (hasRealPass && real != null) {
                storeFile = real
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD") ?: (project.findProperty("RELEASE_KEYSTORE_PASSWORD") as String? ?: "")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: (project.findProperty("RELEASE_KEY_ALIAS") as String? ?: "dotnotes")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: (project.findProperty("RELEASE_KEY_PASSWORD") as String? ?: "")
            } else if (hasRealPass) {
                // CI decoded to app/release.keystore
                val ciFile = file("release.keystore")
                storeFile = if (ciFile.exists()) ciFile else file("${System.getProperty("user.home")}/.android/debug.keystore")
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD") ?: (project.findProperty("RELEASE_KEYSTORE_PASSWORD") as String? ?: "")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: (project.findProperty("RELEASE_KEY_ALIAS") as String? ?: "dotnotes")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: (project.findProperty("RELEASE_KEY_PASSWORD") as String? ?: "")
            } else {
                // fallback debug (local dev without real keystore env)
                storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    kotlinOptions {
        // ponytail: keep release debuggable despite experimental APIs; debug already ok, release was killing build
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi"
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.documentfile)
    implementation(libs.reorderable)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("io.coil-kt:coil-compose:2.7.0")
}