// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.10.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.51")
        classpath("com.google.gms:google-services:4.4.0")
        // KSP plugin is applied in the app-level build.gradle.kts
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { 
            url = uri("https://androidx.dev/storage/health/connect/releases/m2/")
            content {
                includeGroupByRegex("androidx\\.health.*")
            }
        }
        maven { url = uri("https://androidx.dev/storage/compose-compiler/repository/") }
    }
}