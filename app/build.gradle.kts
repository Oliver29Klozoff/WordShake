import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Signing details live in local.properties, which is untracked. The fallback to
// the debug keystore keeps a release build working on a machine without the
// release key — but an APK signed that way cannot update an existing install,
// so releases must be cut on a machine that has it.
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.mj.wordshake"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mj.wordshake"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.3"
    }

    signingConfigs {
        create("release") {
            val home = System.getProperty("user.home")
            storeFile = file(
                localProps.getProperty("RELEASE_STORE_FILE") ?: "$home/.android/debug.keystore"
            )
            storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD") ?: "android"
            keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS") ?: "androiddebugkey"
            keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD") ?: "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
