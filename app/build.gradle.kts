plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val debugApiBaseUrl = providers.gradleProperty("debugApiBaseUrl")
    .getOrElse("http://127.0.0.1:8787/")
    .let { if (it.endsWith("/")) it else "$it/" }

val releaseApiBaseUrl = providers.gradleProperty("releaseApiBaseUrl")
    .getOrElse("https://api.automatesmartbot.com/")
    .let { if (it.endsWith("/")) it else "$it/" }

android {
    namespace = "com.example.automatesmartbot"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.automatesmartbot"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        manifestPlaceholders["usesCleartextTraffic"] = "false"
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"$debugApiBaseUrl\"")
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BASE_URL", "\"$releaseApiBaseUrl\"")
            manifestPlaceholders["usesCleartextTraffic"] = "false"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0}{LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofitConverterGson)
    implementation(libs.okhttp)
    implementation(libs.loggingInterceptor)
    implementation(libs.androidx.security.crypto)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
