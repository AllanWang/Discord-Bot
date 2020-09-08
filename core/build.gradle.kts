import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version bot.Versions.kotlin
}

dependencies {
    implementation(kotlin("stdlib"))
    api(bot.Dependencies.kord)
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = bot.Gradle.jvmTarget
        freeCompilerArgs = bot.Gradle.compilerArgs
    }
}