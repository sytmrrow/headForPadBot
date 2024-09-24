// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    kotlin("kapt") version "1.9.10" apply false // 添加 kotlin-kapt 插件，版本号根据你的项目调整

}

allprojects {

}
