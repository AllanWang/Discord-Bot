import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version bot.Versions.kotlin
    kotlin("kapt") version bot.Versions.kotlin
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(bot.Dependencies.dagger)
    kapt(bot.Dependencies.daggerKapt)

    implementation(bot.Dependencies.koin)
//    implementation(bot.Dependencies.koin("core-ext"))
//    implementation(bot.Dependencies.kordxCommands)
//    kapt(bot.Dependencies.kordxCommandsKapt)

    implementation(project(":core"))
    implementation(project(":base"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = bot.Gradle.jvmTarget
        freeCompilerArgs = bot.Gradle.compilerArgs
    }
}