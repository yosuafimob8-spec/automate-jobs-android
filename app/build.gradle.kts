plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val debugApiBaseUrl = providers.gradleProperty("debugApiBaseUrl")
    // 10.0.2.2 routes an Android emulator to the development host. A physical
    // device must receive an explicit, secured LAN/tunnel URL via this property.
    .getOrElse("http://10.0.2.2:8787/")
    .let { if (it.endsWith("/")) it else "$it/" }

val releaseApiBaseUrl = providers.gradleProperty("releaseApiBaseUrl")
    .getOrElse("https://api.automatesmartbot.com/")
    .let { if (it.endsWith("/")) it else "$it/" }

val debugJobsV2BaseUrl = providers.gradleProperty("debugJobsV2BaseUrl")
    .getOrElse("http://10.0.2.2:8788/")
    .let { if (it.endsWith("/")) it else "$it/" }
val releaseJobsV2BaseUrl = providers.gradleProperty("releaseJobsV2BaseUrl")
    .orNull
    ?.let { if (it.endsWith("/")) it else "$it/" }
    ?: ""
val debugPsychotestBaseUrl = providers.gradleProperty("debugPsychotestBaseUrl")
    .getOrElse("http://10.0.2.2:8789/")
    .let { if (it.endsWith("/")) it else "$it/" }

val releasePsychotestBaseUrl = providers.gradleProperty("releasePsychotestBaseUrl")
    .orNull?.let {
        require(it.startsWith("https://")) { "Release psychotest URL must use HTTPS" }
        if (it.endsWith("/")) it else "$it/"
    }.orEmpty()

val debugInterviewBaseUrl = providers.gradleProperty("debugInterviewBaseUrl").getOrElse("http://127.0.0.1:8790/")
val releaseInterviewBaseUrl = providers.gradleProperty("releaseInterviewBaseUrl").getOrElse("")

val debugTrainingBaseUrl = providers.gradleProperty("debugTrainingBaseUrl").getOrElse("")
val releaseTrainingBaseUrl = providers.gradleProperty("releaseTrainingBaseUrl").getOrElse("")
require(debugTrainingBaseUrl.isEmpty() || debugTrainingBaseUrl.startsWith("https://"))
require(releaseTrainingBaseUrl.isEmpty() || releaseTrainingBaseUrl.startsWith("https://"))

val generatedBrandRes = layout.buildDirectory.dir("generated/res/karirMate")
val prepareBrandAssets by tasks.registering(Copy::class) {
    from(rootProject.file("design/Logo.jpeg"))
    into(generatedBrandRes.map { it.dir("drawable") })
    rename { "karir_mate_logo.jpeg" }
}

android {
    namespace = "com.example.automatesmartbot"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.automatesmartbot"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
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
            buildConfigField("String", "JOBS_V2_BASE_URL", "\"$debugJobsV2BaseUrl\"")
            buildConfigField("String", "PSYCHOTEST_BASE_URL", "\"$debugPsychotestBaseUrl\"")
            buildConfigField("String", "INTERVIEW_BASE_URL", "\"$debugInterviewBaseUrl\"")
            buildConfigField("String", "TRAINING_BASE_URL", "\"$debugTrainingBaseUrl\"")
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        create("termux") {
            initWith(getByName("debug"))
            isDebuggable = true
            matchingFallbacks += listOf("debug")
            buildConfigField("String", "BASE_URL", "\"http://127.0.0.1:8787/\"")
            buildConfigField("String", "JOBS_V2_BASE_URL", "\"http://127.0.0.1:8788/\"")
            buildConfigField("String", "PSYCHOTEST_BASE_URL", "\"http://127.0.0.1:8789/\"")
            buildConfigField("String", "INTERVIEW_BASE_URL", "\"http://127.0.0.1:8790/\"")
            buildConfigField("String", "TRAINING_BASE_URL", "\"$debugTrainingBaseUrl\"")
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BASE_URL", "\"$releaseApiBaseUrl\"")
            buildConfigField("String", "JOBS_V2_BASE_URL", "\"$releaseJobsV2BaseUrl\"")
            buildConfigField("String", "PSYCHOTEST_BASE_URL", "\"$releasePsychotestBaseUrl\"")
            buildConfigField("String", "INTERVIEW_BASE_URL", "\"$releaseInterviewBaseUrl\"")
            buildConfigField("String", "TRAINING_BASE_URL", "\"$releaseTrainingBaseUrl\"")
            manifestPlaceholders["usesCleartextTraffic"] = "false"
        }
    }
    testOptions { unitTests.isIncludeAndroidResources = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets {
        getByName("main").res.srcDir(generatedBrandRes.get().asFile)
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0}{LGPL2.1}"
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn(prepareBrandAssets)
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
    implementation("com.google.android.gms:play-services-ads:25.5.0")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation(libs.retrofit)
    implementation(libs.retrofitConverterGson)
    implementation(libs.okhttp)
    implementation(libs.loggingInterceptor)
    implementation(libs.androidx.security.crypto)
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation("com.google.zxing:core:3.5.3")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
    testImplementation(libs.junit)
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
