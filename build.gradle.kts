import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version bot.Versions.kotlin
    kotlin("kapt") version bot.Versions.kotlin
    id("com.diffplug.spotless") version bot.Versions.spotless
}

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath(bot.Plugins.spotless)
    }
}

repositories {
    jcenter()
}

subprojects {
    if (projectDir.name == "buildSrc") {
        return@subprojects
    }

    group = "ca.allanwang"
    version = bot.Versions.botVersion

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.kapt")
    apply(plugin = "com.diffplug.spotless")

    repositories {
        jcenter()
        maven(url = "https://dl.bintray.com/kordlib/Kord")
    }

    dependencies {
        implementation(kotlin("stdlib"))
        implementation(bot.Dependencies.dagger)
        kapt(bot.Dependencies.daggerKapt)
        implementation(bot.Dependencies.flogger)
        implementation(bot.Dependencies.coroutines)

        testImplementation(bot.Dependencies.flogger("system-backend"))
        testImplementation(bot.Dependencies.hamkrest)
        testImplementation(kotlin("test-junit5"))
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

    configure<SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            ktlint("0.40.0").userData(mapOf("disabled_rules" to "no-wildcard-imports"))
            trimTrailingWhitespace()
            endWithNewline()
        }
    }

    if (projectDir.name in listOf("core", "base", "firebase")) {
        return@subprojects
    }

    dependencies {
        implementation(project(":core"))
        implementation(project(":base"))
        implementation(project(":firebase"))
    }

}