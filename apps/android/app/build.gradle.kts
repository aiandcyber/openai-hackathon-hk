plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

fun loadProps(file: java.io.File): Map<String, String> {
    if (!file.exists()) return emptyMap()
    return file.readLines()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
        .associate { line ->
            val idx = line.indexOf('=')
            line.substring(0, idx).trim() to line.substring(idx + 1).trim().trim('"').trim('\'')
        }
}

fun escape(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")

val local = loadProps(rootProject.file("local.properties"))
// Emulator → host machine. Physical phone: set API_BASE_URL to your PC/WSL LAN IP or HTTPS deploy URL.
val apiBase =
    local["API_BASE_URL"]
        ?: (project.findProperty("API_BASE_URL") as String?)
        ?: "http://10.0.2.2:3000"
val deviceToken = local["DEVICE_API_TOKEN"] ?: "dev-device-token"

android {
    namespace = "com.aiforseniors.scamshield"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aiforseniors.scamshield"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-hackathon"

        buildConfigField("String", "API_BASE_URL", "\"${escape(apiBase.trimEnd('/'))}\"")
        buildConfigField("String", "DEVICE_API_TOKEN", "\"${escape(deviceToken)}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
}
