import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version bot.Versions.kotlin
    kotlin("kapt") version bot.Versions.kotlin
}

repositories {
    jcenter()
}

apply(plugin = "ca.allanwang.discord.bot.gradle.plugin")

val pluginGenDir = File(buildDir, "plugingen")

subprojects {
    if (projectDir.name == "buildSrc") {
        return@subprojects
    }

    group = "ca.allanwang"
    version = bot.Versions.botVersion

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.kapt")

    repositories {
        jcenter()
        maven(url = "https://dl.bintray.com/kordlib/Kord")
    }

    sourceSets {
        main {
            java {
                srcDir(pluginGenDir)
            }
        }
    }

    dependencies {
        implementation(kotlin("stdlib"))
        implementation(bot.Dependencies.dagger)
        kapt(bot.Dependencies.daggerKapt)
        implementation(bot.Dependencies.kord)
        implementation(bot.Dependencies.kordxEmojis)
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

    if (projectDir.name in listOf("core", "base", "firebase")) {
        return@subprojects
    }

    dependencies {
        implementation(project(":core"))
        implementation(project(":base"))
        implementation(project(":firebase"))
    }

}