package ca.allanwang.discord.bot.echo

import ca.allanwang.discord.bot.base.*
import com.google.common.flogger.FluentLogger
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import dev.kord.common.Color
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EchoBot @Inject constructor() : CommandHandlerBot {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()

        private val embedColor = Color(0xff306EA4.toInt())
    }

    override val handler = commandBuilder("echo", embedColor, CommandHandler.Type.Prefix, description = "... echo") {
        hiddenHelp = true
        action(withMessage = true) {
            echoAction()
        }
    }

    private suspend fun CommandHandlerEvent.echoAction() {
        channel.createMessage(message)
    }
}

@Module(includes = [BotPrefixModule::class])
object EchoBotModule {

    @Provides
    @IntoSet
    @Singleton
    fun bot(bot: EchoBot): CommandHandlerBot = bot
}
