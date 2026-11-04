plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "fr.roadguard.drive"
    compileSdk = 36

    defaultConfig {
        // Nouvelle application, écrite sans code Dashwheel : nouvel identifiant.
        applicationId = "fr.roadguard.drive"
        minSdk = 27 // autoradio de référence : alps 8227L, SDK_INT 27
        targetSdk = 36
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "2.0.0-dev"
        ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a") }
    }

    // Clé de publication fournie par la CI (secrets GitHub), jamais dans le dépôt.
    val keystore = System.getenv("RG_KEYSTORE_FILE")?.let { file(it) }?.takeIf { it.exists() }
    signingConfigs {
        if (keystore != null) {
            create("release") {
                storeFile = keystore
                storePassword = System.getenv("RG_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RG_KEY_ALIAS")
                keyPassword = System.getenv("RG_KEY_PASSWORD")
                enableV1Signing = true // vieux installeurs d'autoradio
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    testOptions {
        unitTests.all {
            // Résultat de chaque test visible dans le journal de compilation (dont le trajet hors ligne).
            it.testLogging { events("passed", "skipped", "failed"); showStandardStreams = true }
        }
    }
    lint {
        abortOnError = true; checkReleaseBuilds = false
        // GraphHopper référence des classes Java de bureau jamais utilisées par l'itinéraire voiture.
        disable += setOf("InvalidPackage")
    }
    packaging {
        resources {
            // Bibliothèques Java (GraphHopper, Jackson…) : fichiers de licence en double.
            excludes += setOf("META-INF/DEPENDENCIES", "META-INF/LICENSE*", "META-INF/NOTICE*", "META-INF/INDEX.LIST", "META-INF/versions/**")
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.01.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("org.maplibre.gl:android-sdk:11.11.0")
    // Même client HTTP que MapLibre, pour faire passer la carte par le téléphone si besoin.
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Itinéraires hors ligne (graphe OpenStreetMap précalculé).
    implementation("com.graphhopper:graphhopper-core:5.3") {
        exclude(group = "org.codehaus.janino")
        exclude(group = "org.openstreetmap.osmosis")
        exclude(group = "org.apache.xmlgraphics")
    }
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    // Vrai lecteur JSON pour les tests JVM (celui d Android n existe que sur l appareil).
    testImplementation("org.json:json:20240303")
}

// Bases radars : une seule copie dans le dépôt (celle du GPS), recopiée à la compilation.
val radarAssets = layout.buildDirectory.dir("generated/radarAssets")
val copyRadarAssets by tasks.registering(Copy::class) {
    val gpsAssets = rootProject.file("../gps/app/src/main/assets")
    into(radarAssets)
    from(gpsAssets) { include("radars_embedded_*.csv"); into("radars") }
    // Carte hors ligne : style et polices embarqués, communs avec l'appli GPS.
    from(gpsAssets) { include("maps/local_style.json", "glyphs/**", "offline_map_catalog.json", "graphs/**") }
}
android.sourceSets.getByName("main").assets.srcDir(radarAssets)
// Protocole ROADGUARD Link : même code Java que l'appli GPS du téléphone.
android.sourceSets.getByName("main").java.srcDir(rootProject.file("../link-protocol/src/main/java"))
// Cœur commun ROADGUARD (carte, radars, navigation, musique, réglages) : même code que l'appli GPS.
android.sourceSets.getByName("main").java.srcDir(rootProject.file("../core/src/main/java"))
tasks.named("preBuild") { dependsOn(copyRadarAssets) }
