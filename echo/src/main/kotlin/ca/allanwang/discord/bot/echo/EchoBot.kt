package ca.allanwang.discord.bot.echo

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
class EchoBot @Inject constructor(
) : BotFeature {

    val logger = FluentLogger.forEnclosingClass()

    override suspend fun Kord.attach() {
        on<MessageCreateEvent> {
            if (message.author?.isBot == true) return@on
            if (message.content == "!ping") message.channel.createMessage("pong")
        }
    }
}

@Module
object EchoBotModule {
    @Provides
    @IntoSet
    @Singleton
    fun botFeature(echoBot: EchoBot): BotFeature = echoBot
}