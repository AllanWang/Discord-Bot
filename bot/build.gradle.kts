import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version bot.Versions.kotlin
    kotlin("kapt") version bot.Versions.kotlin
    java
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":core"))
    implementation(project(":base"))
    implementation(project(":echo"))
    implementation(project(":firebase"))
    implementation(project(":time"))
    implementation(project(":maps"))


    implementation(bot.Dependencies.dagger)
    kapt(bot.Dependencies.daggerKapt)

//    implementation(bot.Dependencies.kordxCommands)
//    kapt(bot.Dependencies.kordxCommandsKapt)

    implementation(bot.Dependencies.slf4j("simple"))
//    implementation(bot.Dependencies.flogger("slf4j-backend"))
    implementation(bot.Dependencies.flogger("system-backend"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = bot.Gradle.jvmTarget
        freeCompilerArgs = bot.Gradle.compilerArgs
    }
}

val fatJar = task("fatJar", type = Jar::class) {
    archiveBaseName.set("${project.name}-fat")
    manifest {
        attributes["Implementation-Title"] = "Discord Bot Jar"
        attributes["Implementation-Version"] = bot.Versions.botVersion
        attributes["Main-Class"] = "ca.allanwang.discord.bot.Bot"
    }
    exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}

tasks {
    "build" {
        dependsOn(fatJar)
    }
}