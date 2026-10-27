import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Server address per build type. Override in local.properties (not committed), e.g.
//   nowme.baseUrl=http://192.168.1.50:25565/   (debug build on a real phone in the same Wi-Fi)
//   nowme.releaseBaseUrl=https://api.example.com/
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun nowmeProperty(name: String, default: String): String =
    (project.findProperty(name) as String?) ?: localProperties.getProperty(name) ?: default

android {
    namespace = "com.example.nowme"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.nowme"
        minSdk = 32
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"${nowmeProperty("nowme.baseUrl", "http://10.0.2.2:25565/")}\"")
            // Plain HTTP is only allowed for local development servers
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        release {
            buildConfigField("String", "BASE_URL", "\"${nowmeProperty("nowme.releaseBaseUrl", "https://nowme.invalid/")}\"")
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    sourceSets {
        getByName("main") {
            res.srcDirs(
                "src/main/res",
                "src/main/res-design",
                "src/main/res-ui/auth",
                "src/main/res-ui/camera",
                "src/main/res-ui/home",
                "src/main/res-ui/main",
                "src/main/res-ui/nowme",
                "src/main/res-ui/profile",
                "src/main/res-ui/shared"
            )
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // CameraX
    implementation("androidx.camera:camera-core:1.3.3")
    implementation("androidx.camera:camera-camera2:1.3.3")
    implementation("androidx.camera:camera-lifecycle:1.3.3")
    implementation("androidx.camera:camera-view:1.3.3")

    // Future listener (CameraProvider)
    implementation("com.google.guava:guava:32.1.3-android")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Security Crypto for EncryptedSharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Glide for memory-efficient image loading and caching
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.github.bumptech.glide:okhttp3-integration:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
}
