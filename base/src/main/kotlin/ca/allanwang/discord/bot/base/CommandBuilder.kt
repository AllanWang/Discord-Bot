package ca.allanwang.discord.bot.base

import dev.kord.common.Color
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.EmbedBuilder
import java.util.*

typealias CommandHandlerAction = suspend CommandHandlerEvent.() -> Unit

data class CommandHandlerEvent(
    val event: MessageCreateEvent,
    val prefix: String,
    val type: CommandHandler.Type,
    val command: String,
    val message: String,
    val origMessage: String,
    val commandHelp: CommandHelp,
) {
    val channel get() = event.message.channel
    val authorId get() = event.message.author?.id

    fun EmbedBuilder.userFooter() {
        footer
        val tag = event.message.author?.tag
        if (tag != null) footer {
            text = tag
        }
    }
}

interface CommandHelp {
    val hiddenHelp: Boolean

    suspend fun handleHelp(event: CommandHandlerEvent)

    fun help(context: HelpContext): List<String>
}

interface CommandHandler : CommandHelp {

    val description: String?

    val types: Set<Type>

    val command: String

    fun topLevelHelp(context: HelpContext): String?

    suspend fun handle(event: CommandHandlerEvent)

    enum class Type {
        Prefix, Mention
    }
}

interface CommandHandlerBot {
    suspend fun Kord.attach(): Unit = Unit
    val embedColor: Color
    val handler: CommandHandler
}

internal fun Collection<CommandHandlerBot>.withType(type: CommandHandler.Type): Collection<CommandHandlerBot> =
    filter { type in it.handler.types }

/**
 * Returns single map of command handlers based on their supported keys
 */
internal fun Collection<CommandHandlerBot>.candidates(): Map<String, CommandHandler> =
    map { it.handler }.associateBy { it.command.toLowerCase(Locale.US) }

internal fun Collection<CommandHandlerBot>.duplicateKeys(): Set<String> =
    map { it.handler.command.toLowerCase(Locale.US) }
        .groupBy { it }
        .filter { it.value.size > 1 }.keys
