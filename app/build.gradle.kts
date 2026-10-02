import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

fun prop(name: String, default: String) = (project.findProperty(name) as String?)?.trim()?.takeIf { it.isNotEmpty() } ?: default
val keystore = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.mechvac.task"
    compileSdk = 34

    defaultConfig {
        applicationId = prop("APP_ID", "com.mechvac.task")
        minSdk = 26
        targetSdk = 34
        versionCode = prop("VERSION_CODE", "1").toInt()
        versionName = prop("VERSION_NAME", "1.0")
        val url = prop("APP_URL", "").trimEnd('/').let { if (it.contains("your-website.com")) "" else it }
        buildConfigField("String", "APP_URL", "\"" + url + "\"")
        resValue("string", "app_name", prop("APP_NAME", "MechVac Task"))
        manifestPlaceholders["allowHttp"] = prop("ALLOW_HTTP", "false")
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file(keystore.getProperty("storeFile", "mechvac-task-release.jks"))
            storePassword = keystore.getProperty("storePassword")
            keyAlias = keystore.getProperty("keyAlias")
            keyPassword = keystore.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    // Instant notifications (Firebase Cloud Messaging). The Firebase project is set up on the website
    // (Company settings > Phone app), so no google-services.json is needed inside the app.
    implementation("com.google.firebase:firebase-messaging:23.4.1")
}
