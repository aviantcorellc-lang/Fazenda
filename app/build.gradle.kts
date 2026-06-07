import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val versionFile = rootProject.file("app/version.properties")
val versionProps = Properties()
versionFile.inputStream().use { versionProps.load(it) }

android {
    namespace = "com.fazenda.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fazenda.app"
        minSdk = 26
        targetSdk = 35
        versionCode = versionProps.getProperty("VERSION_CODE").toInt()
        versionName = versionProps.getProperty("VERSION_NAME")
        buildConfigField("String", "GITHUB_REPO", "\"aviantcorellc-lang/Fazenda\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file("fazenda-keystore.jks")
            storePassword = System.getenv("FAZENDA_STORE_PASSWORD") ?: ""
            keyAlias = "fazenda-key"
            keyPassword = System.getenv("FAZENDA_KEY_PASSWORD") ?: System.getenv("FAZENDA_STORE_PASSWORD") ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val envPassword = System.getenv("FAZENDA_STORE_PASSWORD")
            if (!envPassword.isNullOrEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
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
        buildConfig = true
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.coil.compose)
    implementation(libs.coroutines.android)
    implementation(libs.osmdroid.android)
    debugImplementation(libs.compose.ui.tooling)
}
