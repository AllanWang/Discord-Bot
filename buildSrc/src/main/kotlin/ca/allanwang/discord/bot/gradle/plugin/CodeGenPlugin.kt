package ca.allanwang.discord.bot.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class CodeGenPlugin  : Plugin<Project> {

    override fun apply(target: Project) {
        println("Hi project asdf")
    }

}