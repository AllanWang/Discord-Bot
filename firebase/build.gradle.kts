import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version bot.Versions.kotlin
    kotlin("kapt") version bot.Versions.kotlin
}

dependencies {
    implementation(kotlin("stdlib"))
    api(bot.Dependencies.firebase("admin"))
    api(bot.Dependencies.flogger)
    api(bot.Dependencies.coroutines)
    implementation(bot.Dependencies.dagger)
    kapt(bot.Dependencies.daggerKapt)
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = bot.Gradle.jvmTarget
        freeCompilerArgs = bot.Gradle.compilerArgs
    }
}