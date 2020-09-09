package ca.allanwang.discord.bot.game

import ca.allanwang.discord.bot.core.create
import com.gitlab.kordlib.core.Kord
import com.google.common.flogger.FluentLogger

suspend fun main(args: Array<String>) {

    val logger = FluentLogger.forEnclosingClass()

    val kord = Kord.create(args)

    logger.atInfo().log("Initialized Game Bot")

    kord.gameBot()

    kord.login { playing("!game") }
}
