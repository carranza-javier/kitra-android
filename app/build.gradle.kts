import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Upload key for Google Play. Never committed: read from environment variables or from a properties
// file outside the repository (default: ~/.kitra/keystore.properties, override with KITRA_KEYSTORE_PROPERTIES).
val keystoreProperties = Properties().apply {
    val path = System.getenv("KITRA_KEYSTORE_PROPERTIES")
        ?: "${System.getProperty("user.home")}/.kitra/keystore.properties"
    val file = File(path)
    if (file.exists()) file.inputStream().use { load(it) }
}
fun signingValue(env: String, property: String): String? = System.getenv(env) ?: keystoreProperties.getProperty(property)
val uploadStoreFile = signingValue("KITRA_STORE_FILE", "storeFile")

android {
    namespace = "com.kettenki.kitra"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.kettenki.kitra"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("upload") {
            if (uploadStoreFile != null) {
                storeFile = file(uploadStoreFile)
                storePassword = signingValue("KITRA_STORE_PASSWORD", "storePassword")
                keyAlias = signingValue("KITRA_KEY_ALIAS", "keyAlias")
                keyPassword = signingValue("KITRA_KEY_PASSWORD", "keyPassword")
            }
        }
    }

    // Same code, two distributions:
    // - full: READ_MEDIA_IMAGES + automatic screenshot detection (one tap). Needs the Play permission declaration.
    // - play: no photo permission; "Translate" notice opens the system photo picker, plus the share target.
    flavorDimensions += "distribution"
    productFlavors {
        create("full") {
            dimension = "distribution"
            versionCode = 1
            buildConfigField("boolean", "BROAD_PHOTO_ACCESS", "true")
        }
        create("play") {
            dimension = "distribution"
            // Higher than "full": if Play rejects the full build, versionCode 1 is already used by that upload.
            versionCode = 2
            buildConfigField("boolean", "BROAD_PHOTO_ACCESS", "false")
        }
    }

    buildTypes {
        release {
            signingConfig = if (uploadStoreFile != null) signingConfigs.getByName("upload") else null
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // ML Kit on-device translation ships ~16 MB of native code per ABI. Play (AAB) delivers only the
    // device's ABI; for standalone APKs we build one APK per ABI.
    splits {
        abi {
            // AGP cannot build per-ABI APKs and an AAB in the same invocation (issuetracker 402800800).
            isEnable = gradle.startParameter.taskNames.none { it.contains("bundle", ignoreCase = true) }
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = false
        }
    }

    lint {
        // The UI is translated (es/de); lint's English spell checker flags words such as "momento".
        disable += "Typos"
        warningsAsErrors = false
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    // On-device engines. Unbundled OCR and language ID: Google Play services downloads the models,
    // so they do not grow the APK.
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.mlkit.genai.prompt)
    implementation(libs.mlkit.translate)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.text.recognition.chinese)
    implementation(libs.mlkit.language.id)
    testImplementation(libs.junit)
}
