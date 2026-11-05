buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.5.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24")
        classpath("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:1.9.20-1.0.14")
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}