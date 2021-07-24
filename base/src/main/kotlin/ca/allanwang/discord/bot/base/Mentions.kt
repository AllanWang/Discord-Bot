package ca.allanwang.discord.bot.base

import dev.kord.common.entity.Snowflake
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Mentions @Inject constructor() {
    val channelMentionRegex = Regex("<#([0-9]+)>")
    fun channelMention(snowflake: Snowflake) = "<#${snowflake.value}>"

    /**
     * Discord ids are sent via text via `<@{id}>`.
     * If there is a nickname, an additional `!` will follow `@`
     */
    val userMentionRegex = Regex("<@!?([0-9]+)>")
    fun userMention(snowflake: Snowflake) = "<@${snowflake.value}>"

    val roleMentionRegex = Regex("<@&([0-9]+)>")
    fun roleMention(snowflake: Snowflake) = "<@&${snowflake.value}>"
}
