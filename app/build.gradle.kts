import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.jlleitschuhGradleKtlint)
    alias(libs.plugins.daggerHiltAndroid)
    kotlin("kapt")
}

android {
    namespace = "com.tomtom.buster"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.tomtom.buster"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildTypes.configureEach {
        val properties = Properties()
        properties.load(FileInputStream(rootProject.file("local.properties")))
        buildConfigField("String", "TOMTOM_API_KEY", "\"${properties.getProperty("TOMTOM_API_KEY")}\"")
    }
}

dependencies {

    implementation(libs.androidx.fragment)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    implementation(libs.navsdk.provider.android)
    implementation(libs.navsdk.provider.map.matched)
    implementation(libs.navsdk.provider.simulation)
    implementation(libs.navsdk.map.display)
    implementation(libs.navsdk.navigation.tile.store)
    implementation(libs.navsdk.navigation.online)
    implementation(libs.navsdk.route.replanner)
    implementation(libs.navsdk.ui)
    implementation(libs.navsdk.route.planner)
    implementation(libs.androidx.lifecycle)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.rules)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    testImplementation(libs.mockk)
    testImplementation(libs.androidx.core.testing)

    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    implementation(project(":navsdkextensions"))

    implementation(libs.dagger)
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)

    androidTestImplementation(libs.androidx.fragment)
}

kapt {
    correctErrorTypes = true
}
