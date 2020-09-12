package ca.allanwang.discord.bot.base

import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.rest.builder.message.EmbedBuilder

typealias CommandHandlerAction = suspend CommandHandlerEvent.() -> Unit

class CommandHandlerEvent(val event: MessageCreateEvent, val command: String, val message: String) {
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

interface CommandHandler {

    val types: Set<Type>

    val keys: Set<String>

    suspend fun handle(event: MessageCreateEvent, message: String)

    enum class Type {
        Prefix, Mention
    }
}

interface CommandHandlerBot {
    val handler: CommandHandler
}

internal fun Collection<CommandHandlerBot>.withType(type: CommandHandler.Type): Collection<CommandHandlerBot> =
    filter { type in it.handler.types }

/**
 * Returns single map of command handlers based on their supported keys
 */
internal fun Collection<CommandHandlerBot>.candidates(): Map<String, CommandHandler> =
    map { it.handler }
        .flatMap { handler -> handler.keys.map { it to handler } }
        .toMap()

internal fun Collection<CommandHandlerBot>.duplicateKeys(): Set<String> =
    map { it.handler }
        .flatMap { it.keys }
        .groupBy { it }
        .filter { it.value.size > 1 }.keys
