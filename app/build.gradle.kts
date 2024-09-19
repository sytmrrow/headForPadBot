plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
}

android {
    namespace = "com.example.application_for_head_913"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.application_for_head_913"
        minSdk = 24
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
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("lib")
        }
    }
}
android {
    defaultConfig {
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }
}



dependencies {
    implementation("com.google.android.material:material:1.4.0")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation(fileTree(mapOf("dir" to "lib", "include" to listOf("*.jar", "*.aar"))))
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}