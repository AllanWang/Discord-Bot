buildscript {
//    repositories {
//        jcenter()
//    }
//
//    dependencies {
//        classpath(bot.Plugins.kotlin)
//    }
}

//plugins {
//    kotlin("jvm") version bot.Versions.kotlin
//}

subprojects {
    if (projectDir.name == "buildSrc") {
        return@subprojects
    }

    group = "ca.allanwang"
    version = bot.Versions.botVersion

    repositories {
        mavenCentral()
        maven(url = "https://dl.bintray.com/kordlib/Kord")
    }
}