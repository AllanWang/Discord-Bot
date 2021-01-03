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
