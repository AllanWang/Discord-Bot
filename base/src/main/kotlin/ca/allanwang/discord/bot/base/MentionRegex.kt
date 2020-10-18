package ca.allanwang.discord.bot.base

import com.gitlab.kordlib.common.entity.Snowflake
import javax.inject.Singleton

@Singleton
class MentionRegex {
    val channelMentionRegex = Regex("<#([0-9]+)>")
    fun channelMention(snowflake: Snowflake) = "<#${snowflake.value}>"

    val userMentionRegex = Regex("<@!?([0-9]+)>")
    fun userMentionRegex(snowflake: Snowflake) = "<@${snowflake.value}>"

    val roleMentionRegex = Regex("<@&([0-9]+)>")
    fun roleMention(snowflake: Snowflake) = "<@&${snowflake.value}>"
}