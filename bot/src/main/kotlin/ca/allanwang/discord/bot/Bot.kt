package ca.allanwang.discord.bot

import ca.allanwang.discord.bot.echo.echoBot
import com.gitlab.kordlib.core.Kord

suspend fun main(args: Array<String>) {
    val kord = Kord(args.firstOrNull() ?: error("token required"))

    kord.echoBot()

    kord.login { playing("!ping to pong") }
}