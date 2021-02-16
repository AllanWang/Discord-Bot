package ca.allanwang.discord.bot.core

interface Build {
    val valid: Boolean
    val version: String
    val buildTime: String
    val commitUrl: String
}
