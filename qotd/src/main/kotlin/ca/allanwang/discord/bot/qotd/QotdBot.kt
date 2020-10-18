package ca.allanwang.discord.bot.qotd

import ca.allanwang.discord.bot.base.CommandHandler
import ca.allanwang.discord.bot.base.CommandHandlerBot
import ca.allanwang.discord.bot.base.commandBuilder
import com.gitlab.kordlib.core.Kord
import com.google.common.flogger.FluentLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QotdBot @Inject constructor(
) : CommandHandlerBot {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    override suspend fun Kord.attach() {

    }

    override val handler = commandBuilder(CommandHandler.Type.Prefix) {
        arg("qotd") {

        }
    }


}