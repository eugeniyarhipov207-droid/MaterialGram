import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

// Читаем local.properties
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use {
            load(it)
        }
    }
}

val telegramApiId = localProperties.getProperty("TELEGRAM_API_ID", "0")
val telegramApiHash = localProperties.getProperty("TELEGRAM_API_HASH", "")

// Настройки подписи релиза
val keystorePropsFile = rootProject.file("release.properties")

val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use {
            load(it)
        }
    }
}

val signingKeys = listOf(
    "storeFile",
    "storePassword",
    "keyAlias",
    "keyPassword"
)

val hasValidSigningProps = signingKeys.all {
    !keystoreProps.getProperty(it).isNullOrBlank()
}

android {
    namespace = "com.kyecg.materialgram"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.kyecg.materialgram"
        minSdk = 28
        targetSdk = 36

        versionCode = 1
        versionName = "0.1.0"

        vectorDrawables {
            useSupportLibrary = true
        }

        // Telegram API credentials
        buildConfigField(
            "int",
            "TELEGRAM_API_ID",
            telegramApiId
        )

        buildConfigField(
            "String",
            "TELEGRAM_API_HASH",
            "\"$telegramApiHash\""
        )
    }

    signingConfigs {
        if (hasValidSigningProps) {
            create("release") {
                storeFile = rootProject.file(
                    keystoreProps.getProperty("storeFile")
                )
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }

        release {
            if (hasValidSigningProps) {
                signingConfig = signingConfigs.getByName("release")
            }

            isMinifyEnabled = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
        viewBinding = false
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/kotlinx_coroutines_core.version"
        }
    }

    lint {
        checkReleaseBuilds = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(
            org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        )
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)

    implementation(composeBom)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Telegram TDLib
    implementation("io.github.tdlib-android:core:0.1.1")
    implementation("io.github.tdlib-android:ktx:0.1.1")

    // Phone number utilities
    implementation("com.googlecode.libphonenumber:libphonenumber:9.0.43")

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
