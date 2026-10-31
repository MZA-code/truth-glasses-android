plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.mzacode.truthglasses"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.mzacode.truthglasses"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    // Signature de la release : fournie par la CI (secrets du dépôt, voir release.yml) ou en local
    // par des variables d'environnement. Sans elles, assembleRelease produit un APK non signé.
    val keystorePath = providers.environmentVariable("TG_KEYSTORE_PATH").orNull
    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = providers.environmentVariable("TG_KEYSTORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("TG_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("TG_KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // Concerne les App Bundles du Play Store : l'appli est distribuée en APK sur GitHub
        disable += "AppBundleLocaleChanges"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)

    testImplementation(libs.junit)
}
