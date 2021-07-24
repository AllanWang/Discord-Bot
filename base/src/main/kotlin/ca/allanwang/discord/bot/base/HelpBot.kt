package ca.allanwang.discord.bot.base

import ca.allanwang.discord.bot.core.BotFeature
import com.google.common.flogger.FluentLogger
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import dev.kord.common.Color
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HelpBot @Inject constructor(
    private val kord: Kord,
    colorPalette: ColorPalette,
    private val prefixSupplier: BotPrefixSupplier,
    private val mentions: Mentions,
    handlers: Set<@JvmSuppressWildcards CommandHandlerBot>,
    private val commandBots: Set<@JvmSuppressWildcards CommandBot>
) : BotFeature {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    private val embedColor: Color = colorPalette.default

    private val handlers = CommandHandler.Type.values().associateWith { type ->
        handlers.map { it.handler }
            .filter { it.types.contains(type) }
            .sortedBy { it.command }
    }.filterValues { it.isNotEmpty() }

    /**
     * Reuse commandBots for prefix handling. Cannot use command handler bots due to cyclic dependencies.
     */
    override suspend fun Kord.attach() {
        onMessage {
            val prefix = prefixSupplier.prefix(groupSnowflake())
            if (commandBots.any {
                with(it) {
                    prefixedMessage()?.message?.equals("help", ignoreCase = true) == true
                }
            }
            ) {
                kord.launch {
                    help(prefix)
                }
            }
        }
    }

    private suspend fun MessageCreateEvent.help(prefix: String) {
        val pages =
            handlers.flatMap { group ->
                val truePrefix = when (group.key) {
                    CommandHandler.Type.Prefix -> prefix
                    CommandHandler.Type.Mention -> mentions.userMention(kord.selfId)
                }
                val helpContext = HelpContext(prefix = truePrefix, type = group.key)
                group.value.filter { !it.hiddenHelp }.mapNotNull { bot -> bot.topLevelHelp(helpContext) }
            }.chunkedByLength(1024)
        message.channel.paginatedMessage(pages) { desc ->
            title = "Help"
            color = embedColor
            description = buildString {
                append("List of commands. Call ")
                appendCodeBlock { append("[command] help") }
                append(" for more info on a specific command.")
            }
            field {
                value = desc ?: "No commands found"
            }
        }
    }
}

@Module(includes = [CommandBotModule::class, BotPrefixModule::class])
object HelpBotModule {

    @Provides
    @IntoSet
    @Singleton
    fun bot(bot: HelpBot): BotFeature = bot
}
