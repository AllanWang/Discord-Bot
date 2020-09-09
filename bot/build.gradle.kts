import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version bot.Versions.kotlin
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":core"))
    implementation(project(":echo"))
    implementation(bot.Dependencies.slf4j("simple"))
    implementation(bot.Dependencies.flogger("slf4j-backend"))
    implementation(bot.Dependencies.flogger("system-backend"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = bot.Gradle.jvmTarget
        freeCompilerArgs = bot.Gradle.compilerArgs
    }
}