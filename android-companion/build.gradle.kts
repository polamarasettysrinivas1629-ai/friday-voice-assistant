plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.srinivas.marklvii"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.srinivas.marklvii"
        minSdk = 23
        targetSdk = 35
        versionCode = 10
        versionName = "1.9.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}
