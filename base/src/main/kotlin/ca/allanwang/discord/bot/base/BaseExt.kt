package ca.allanwang.discord.bot.base

import com.gitlab.kordlib.common.entity.Snowflake
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.on

/**
 * Gets snowflake for server or channel for DMs
 */
suspend fun MessageCreateEvent.groupSnowflake(): Snowflake =
    getGuild()?.id ?: message.channelId

fun Kord.onMessage(consumer: suspend MessageCreateEvent.() -> Unit) = on<MessageCreateEvent> {
    if (message.author?.isBot == true) return@on
    consumer()
}