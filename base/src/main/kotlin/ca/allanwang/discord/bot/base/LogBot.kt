package ca.allanwang.discord.bot.base

import ca.allanwang.discord.bot.core.BotFeature
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
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
        onMessage {
            logger.atInfo().log("Message Received: %s", message.content)
            message.data.mentions.let { mentions ->
                if (mentions.isNotEmpty())
                    logger.atInfo().log("\tMentions %s", mentions)
            }
        }
    }
}

@Module
object LogBotModule {
    @Provides
    @IntoSet
    @Singleton
    fun bot(bot: LogBot): BotFeature = bot
}