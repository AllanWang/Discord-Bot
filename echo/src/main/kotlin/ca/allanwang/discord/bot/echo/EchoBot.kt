package ca.allanwang.discord.bot.echo

import ca.allanwang.discord.bot.base.BotPrefixModule
import ca.allanwang.discord.bot.base.CommandHandler
import ca.allanwang.discord.bot.base.commandBuilder
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
class EchoPrefixHandler @Inject constructor(
) : CommandHandler {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    private val handler = commandBuilder("echo") {
        action { event, message ->
            event.message.channel.createMessage(message)
        }
    }

    override suspend fun handle(event: MessageCreateEvent, message: String) {
        handler.handle(event, message)
    }
}

@Module(includes = [BotPrefixModule::class])
object EchoBotModule {

    @Provides
    @IntoSet
    @Singleton
    fun botPrefixHandler(handler: EchoPrefixHandler): CommandHandler = handler
}