plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("kotlin-kapt") // 添加 kotlin-kapt 插件
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

    defaultConfig {
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }
}

dependencies {
    implementation("com.google.android.material:material:1.4.0")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    kapt("com.github.bumptech.glide:compiler:4.12.0") // 使用 kapt 代替 annotationProcessor

    implementation(fileTree(mapOf("dir" to "lib", "include" to listOf("*.jar", "*.aar"))))
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation("com.google.android.exoplayer:exoplayer:2.19.0")
    // ExoPlayer 的 UI 组件，用于 PlayerView
    implementation("com.google.android.exoplayer:exoplayer-ui:2.19.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    //    改动：以下三个依赖为新添加
    implementation(files("lib/RobotSerialPort-1.3.0.jar"))
    implementation(files("lib/RobotBaseSDK-1.19.2.jar"))
    implementation("org.greenrobot:eventbus:3.3.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
