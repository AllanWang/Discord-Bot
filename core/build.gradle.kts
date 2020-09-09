import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version bot.Versions.kotlin
    kotlin("kapt") version bot.Versions.kotlin
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(bot.Dependencies.dagger)
    api(bot.Dependencies.coroutines)
    api(bot.Dependencies.kord)
    api(bot.Dependencies.flogger)
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = bot.Gradle.jvmTarget
        freeCompilerArgs = bot.Gradle.compilerArgs
    }
}