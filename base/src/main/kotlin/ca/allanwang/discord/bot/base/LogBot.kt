package ca.allanwang.discord.bot.base

import ca.allanwang.discord.bot.core.BotFeature
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.on
import com.google.common.flogger.FluentLogger
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogBot @Inject constructor(
) : BotFeature {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    override suspend fun Kord.attach() {
        on<MessageCreateEvent> {
            logger.atInfo().log("Message Received: %s", message.content)
            message.data.mentions.let { mentions ->
                if (mentions.isNotEmpty())
                    logger.atInfo().log("\tMentions %s", mentions)
            }
        }
    }
}

@Module(includes = [BotPrefixModule::class])
object LogBotModule {
    @Provides
    @IntoSet
    @Singleton
    fun prefixBot(bot: LogBot): BotFeature = bot
}