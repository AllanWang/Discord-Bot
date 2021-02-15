package ca.allanwang.discord.bot.base

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Message
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on

/**
 * Gets snowflake for server or channel for DMs
 */
fun MessageCreateEvent.groupSnowflake(): Snowflake =
    guildId ?: message.channelId

fun Message.groupSnowflake(guildId: Snowflake?) = guildId ?: channelId

fun Kord.onMessage(consumer: suspend MessageCreateEvent.() -> Unit) = on<MessageCreateEvent> {
    if (message.author?.isBot == true) return@on
    consumer()
}

fun List<String>.chunkedByLength(length: Int = 2048, emptyText: String? = null): List<String> {
    if (isEmpty()) {
        return if (emptyText != null) listOf(emptyText) else emptyList()
    }
    val stringBuilder = StringBuilder()
    val list = mutableListOf<String>()
    forEach {
        if (stringBuilder.length + it.length <= length) {
            if (stringBuilder.isNotEmpty())
                stringBuilder.appendLine()
        } else {
            list.add(stringBuilder.toString())
            stringBuilder.clear()
        }
        stringBuilder.append(it)
    }
    if (stringBuilder.isNotEmpty()) {
        list.add(stringBuilder.toString())
    }
    return list
}