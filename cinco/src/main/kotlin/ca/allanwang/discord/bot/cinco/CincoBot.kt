package ca.allanwang.discord.bot.cinco


import ca.allanwang.discord.bot.base.CommandHandler
import ca.allanwang.discord.bot.base.CommandHandlerBot
import ca.allanwang.discord.bot.base.CommandHandlerEvent
import ca.allanwang.discord.bot.base.commandBuilder
import com.google.common.flogger.FluentLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CincoBot @Inject constructor(
) : CommandHandlerBot {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    override val handler = commandBuilder(CommandHandler.Type.Prefix) {
        arg("cinco") {
            action(withMessage = false) {
                selectVariant()
            }
        }
    }

    private suspend fun CommandHandlerEvent.selectVariant() {

    }
}
