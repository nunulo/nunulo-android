import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun cleanLocalConfigValue(value: String?): String {
    val text = value.orEmpty().trim()
    return text.takeUnless { it.isBlank() || it == "xxxx" || it.startsWith("填写") }.orEmpty()
}

fun readLocalProperty(file: java.io.File, name: String): String {
    if (!file.isFile) return ""
    val properties = Properties()
    file.inputStream().use { properties.load(it) }
    return cleanLocalConfigValue(properties.getProperty(name))
}

fun firstNotBlank(vararg values: String): String = values.firstOrNull { it.isNotBlank() }.orEmpty()

fun buildConfigString(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

fun readAmapAndroidKey(name: String): String = firstNotBlank(
    providers.gradleProperty(name).orNull.orEmpty(),
    providers.environmentVariable(name).orNull.orEmpty(),
    readLocalProperty(rootProject.projectDir.resolve("local.properties"), name),
    readLocalProperty(rootProject.projectDir.parentFile.resolve("local.properties"), name),
)

val amapAndroidKeyDebug = firstNotBlank(
    readAmapAndroidKey("AMAP_ANDROID_KEY_DEBUG"),
    readAmapAndroidKey("AMAP_ANDROID_KEY"),
)
val amapAndroidKeyRelease = readAmapAndroidKey("AMAP_ANDROID_KEY_RELEASE")

android {
    namespace = "com.lumokato.nunulo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lumokato.nunulo"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        manifestPlaceholders["amapApiKey"] = ""
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["amapApiKey"] = amapAndroidKeyDebug
            buildConfigField("String", "AMAP_ANDROID_KEY", buildConfigString(amapAndroidKeyDebug))
        }

        release {
            manifestPlaceholders["amapApiKey"] = amapAndroidKeyRelease
            buildConfigField("String", "AMAP_ANDROID_KEY", buildConfigString(amapAndroidKeyRelease))
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
    implementation("androidx.activity:activity-compose:1.12.0")
    implementation("androidx.compose.ui:ui:1.11.2")
    implementation("androidx.compose.ui:ui-tooling-preview:1.11.2")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("com.amap.api:3dmap:10.0.600")
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling:1.11.2")
}
