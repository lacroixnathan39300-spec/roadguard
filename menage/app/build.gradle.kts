plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Ménage : petite appli qui trouve ce qui prend de la place pour rien (doublons, gros fichiers, captures,
// rafales, médias des messageries, vieux téléchargements, APK) et le met à la corbeille d'Android.
// Volontairement légère : aucune bibliothèque, seulement Android.
android {
    namespace = "fr.roadguard.menage"
    compileSdk = 36
    defaultConfig {
        applicationId = "fr.roadguard.menage"
        minSdk = 30 // corbeille d'Android (MediaStore.createTrashRequest)
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
