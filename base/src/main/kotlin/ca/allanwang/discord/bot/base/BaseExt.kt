package ca.allanwang.discord.bot.base

import com.gitlab.kordlib.common.entity.Snowflake
import com.gitlab.kordlib.core.event.message.MessageCreateEvent

/**
 * Gets snowflake for server or channel for DMs
 */
suspend fun MessageCreateEvent.groupSnowflake(): Snowflake =
    getGuild()?.id ?: message.channelId