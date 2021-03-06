apply(plugin = "ca.allanwang.discord.bot.gradle.plugin")

val pluginGenDir = File(buildDir, "plugingen")

sourceSets {
    main {
        java {
            srcDir(pluginGenDir)
        }
    }
}

dependencies {
    api(bot.Dependencies.kord)
    api(bot.Dependencies.kordxEmojis)
}