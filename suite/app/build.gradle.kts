plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Pilote Suite : petite appli qui installe et met à jour Pilote (téléphone) et Pilote Drive (autoradio)
// depuis le serveur, sans Play Store. Volontairement légère (pas de Compose).
android {
    namespace = "fr.roadguard.suite"
    compileSdk = 36
    defaultConfig {
        applicationId = "fr.roadguard.suite"
        minSdk = 26
        targetSdk = 36
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = "1." + (System.getenv("VERSION_CODE") ?: "0")
    }
    val keystore = System.getenv("RG_KEYSTORE_FILE")?.let { file(it) }?.takeIf { it.exists() }
    signingConfigs {
        if (keystore != null) {
            create("release") {
                storeFile = keystore
                storePassword = System.getenv("RG_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RG_KEY_ALIAS")
                keyPassword = System.getenv("RG_KEY_PASSWORD")
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
