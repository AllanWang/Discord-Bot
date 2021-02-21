dependencies {
    implementation(project(":echo"))
    implementation(project(":firebase"))
    implementation(project(":time"))
    implementation(project(":maps"))
    implementation(project(":random"))
    implementation(project(":oust"))
    implementation(project(":cinco"))
    implementation(project(":qotd"))
    implementation(project(":gameevent"))

    implementation(bot.Dependencies.slf4j("simple"))
//    implementation(bot.Dependencies.flogger("slf4j-backend"))
    implementation(bot.Dependencies.flogger("system-backend"))
}

val fatJar = task("fatJar", type = Jar::class) {
    archiveBaseName.set("${project.name}-release")
    archiveVersion.set("")
    exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}

tasks.withType<Jar> {
    manifest {
        attributes["Implementation-Title"] = "Discord Bot Jar"
        attributes["Implementation-Version"] = bot.Versions.botVersion
        attributes["Main-Class"] = "ca.allanwang.discord.bot.Bot"
    }
}

tasks {
    "build" {
        dependsOn(fatJar)
    }
}