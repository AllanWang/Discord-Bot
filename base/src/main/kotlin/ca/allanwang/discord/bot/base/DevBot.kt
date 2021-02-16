package ca.allanwang.discord.bot.base

import ca.allanwang.discord.bot.core.CoreModule
import ca.allanwang.discord.bot.core.withTimeout
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import dev.kord.core.Kord
import dev.kord.core.behavior.edit
import kotlinx.coroutines.flow.collect
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Collection of commands to expose dev elements in discord
 */
@Singleton
class DevBot @Inject constructor(
    private val kord: Kord
) : CommandHandlerBot {
    override val handler = commandBuilder(CommandHandler.Type.Mention) {
        arg("dev") {
            arg("channel") {
                action(withMessage = false) {
                    channelInfo()
                }
            }
            arg("emoji") {
                action(withMessage = false) {
                    emojiInfo()
                }
            }
        }
    }

    private suspend fun CommandHandlerEvent.channelInfo() {
        channel.createMessage(buildString {
            appendBold { append("Server: ") }
            appendLine(event.guildId?.asString ?: "Not found")
            appendBold { append("Channel: ") }
            appendLine(event.message.channelId.asString)
            appendBold { append("Author: ") }
            append(event.member?.displayName ?: "No name")
            append(" ")
            appendLine(event.member?.id?.asString ?: "No id")
        })
    }

    private suspend fun CommandHandlerEvent.emojiInfo() {
        var message = channel.createMessage("React to emoji to get id")
        message.reactionAddEvents()
            .withTimeout(TimeUnit.MINUTES.toMillis(1))
            .collect {
                message = message.edit {
                    content = buildString {
                        appendLine(message.content)
                        append(it.emoji.mention)
                        append(" ")
                        appendCodeBlock { append(it.emoji.mention.replace("<", "< ").replace(">", " >")) }
                    }
                }
                message.content
            }
    }
}

@Module(includes = [CoreModule::class, BotPrefixModule::class])
object DevBotModule {

    @Provides
    @IntoSet
    @Singleton
    fun bot(bot: DevBot): CommandHandlerBot = bot
}