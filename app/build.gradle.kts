plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.kettenki.kitra"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.kettenki.kitra"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // La traducción local de ML Kit trae ~16 MB de código nativo por arquitectura. Play (AAB) ya entrega
    // a cada móvil solo la suya; para los APK sueltos se genera uno por arquitectura.
    splits {
        abi {
            // AGP no admite APK por arquitectura y AAB en la misma ejecución (issuetracker 402800800).
            isEnable = gradle.startParameter.taskNames.none { it.contains("bundle", ignoreCase = true) }
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = false
        }
    }

    lint {
        // La interfaz está en español; el corrector de lint marca palabras como "momento".
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
    // Motores en el dispositivo (fase 2). OCR e identificación de idioma "sin agrupar": los modelos
    // los descarga Google Play Services y no engordan el APK.
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.mlkit.genai.prompt)
    implementation(libs.mlkit.translate)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.text.recognition.chinese)
    implementation(libs.mlkit.language.id)
    testImplementation(libs.junit)
}
