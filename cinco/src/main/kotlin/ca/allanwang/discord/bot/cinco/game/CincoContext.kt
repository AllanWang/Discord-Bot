package ca.allanwang.discord.bot.cinco.game

data class CincoContext(
    val botPrefix: String,
    val gameRounds: Int,
    val roundTimeout: Long
)