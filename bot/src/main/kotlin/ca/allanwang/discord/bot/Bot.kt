package ca.allanwang.discord.bot

import ca.allanwang.discord.bot.core.create
import ca.allanwang.discord.bot.echo.echoBot
import com.gitlab.kordlib.core.Kord
import com.google.common.flogger.FluentLogger

suspend fun main(args: Array<String>) {

    val logger = FluentLogger.forEnclosingClass()

    val kord = Kord.create(args)

    logger.atInfo().log("Initialized Bot")

    kord.echoBot()

    kord.login { playing("!ping to pong") }
}

