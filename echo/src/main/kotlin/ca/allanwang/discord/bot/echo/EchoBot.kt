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
class EchoBot @Inject constructor(
    colorPalette: ColorPalette
) : CommandHandlerBot {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    override val embedColor: Color = colorPalette.default

    override val handler = commandBuilder("echo", CommandHandler.Type.Prefix, description = "... echo") {
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
