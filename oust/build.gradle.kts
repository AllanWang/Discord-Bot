import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version bot.Versions.kotlin
    kotlin("kapt") version bot.Versions.kotlin
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(bot.Dependencies.dagger)
    kapt(bot.Dependencies.daggerKapt)

    implementation(project(":core"))
    implementation(project(":base"))
    implementation(project(":firebase"))

    testImplementation(bot.Dependencies.flogger("system-backend"))
    testImplementation(bot.Dependencies.hamkrest)
    testImplementation(kotlin("reflect"))
//    testImplementation(kotlin("test-junit5"))
    testImplementation(bot.Dependencies.junit("api"))
    testImplementation(bot.Dependencies.junit("params"))
    testRuntimeOnly(bot.Dependencies.junit("engine"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = bot.Gradle.jvmTarget
        freeCompilerArgs = bot.Gradle.compilerArgs
    }
}