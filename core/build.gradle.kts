plugins {
    kotlin("jvm") version bot.Versions.kotlin
}

group = "ca.allanwang"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://dl.bintray.com/kordlib/Kord")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(bot.Dependencies.kord)
}
