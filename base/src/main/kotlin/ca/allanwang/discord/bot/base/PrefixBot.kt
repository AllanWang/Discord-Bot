package ca.allanwang.discord.bot.base

import com.google.common.flogger.FluentLogger
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrefixBot @Inject constructor(
) : CommandHandlerBot {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    override val handler = commandBuilder(CommandHandler.Type.Mention) {
        arg("prefix") {
            action(withMessage = true) {
                logger.atInfo().log("Prefix action")
                channel.createMessage(message)
            }
        }
    }
}

@Module(includes = [BotPrefixModule::class])
object PrefixBotModule {

    @Provides
    @IntoSet
    @Singleton
    fun bot(bot: PrefixBot): CommandHandlerBot = bot
}