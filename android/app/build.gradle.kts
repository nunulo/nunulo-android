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

fun readAmapLocalConfigValue(name: String): String {
    val configFile = rootProject.projectDir.parentFile.resolve("app/static/config/amap.local.js")
    if (!configFile.isFile) return ""
    val value = Regex("""$name\s*:\s*"([^"]+)"""").find(configFile.readText())?.groupValues?.get(1).orEmpty()
    return cleanLocalConfigValue(value)
}

fun readLocalProperty(file: java.io.File, name: String): String {
    if (!file.isFile) return ""
    val properties = Properties()
    file.inputStream().use { properties.load(it) }
    return cleanLocalConfigValue(properties.getProperty(name))
}

fun firstNotBlank(vararg values: String): String = values.firstOrNull { it.isNotBlank() }.orEmpty()

fun buildConfigString(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val amapAndroidKeyExplicit = firstNotBlank(
    providers.gradleProperty("AMAP_ANDROID_KEY").orNull.orEmpty(),
    providers.environmentVariable("AMAP_ANDROID_KEY").orNull.orEmpty(),
    readLocalProperty(rootProject.projectDir.resolve("local.properties"), "AMAP_ANDROID_KEY"),
    readLocalProperty(rootProject.projectDir.parentFile.resolve("local.properties"), "AMAP_ANDROID_KEY"),
)
val amapAndroidKeyDebug = firstNotBlank(amapAndroidKeyExplicit, readAmapLocalConfigValue("key"))

android {
    namespace = "com.lumokato.dollcheckin"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lumokato.dollcheckin"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        manifestPlaceholders["amapApiKey"] = ""
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["amapApiKey"] = amapAndroidKeyDebug
            buildConfigField("String", "AMAP_ANDROID_KEY", buildConfigString(amapAndroidKeyDebug))
        }

        release {
            manifestPlaceholders["amapApiKey"] = amapAndroidKeyExplicit
            buildConfigField("String", "AMAP_ANDROID_KEY", buildConfigString(amapAndroidKeyExplicit))
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
    debugImplementation("androidx.compose.ui:ui-tooling:1.11.2")
}
