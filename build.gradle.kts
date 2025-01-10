// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlinKsp) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.gms.google.services) apply false
    alias(libs.plugins.google.firebase.crashlytics) apply false
    id("com.google.dagger.hilt.android") version "2.53.1" apply false
    //id("androidx.room") version "2.6.1" apply false
}

buildscript {
    repositories {
        mavenCentral()
        google()
    }
    dependencies {
        classpath (libs.androidx.navigation.safe.args.gradle.plugin.v260)
        classpath (libs.google.services)
    }
}
