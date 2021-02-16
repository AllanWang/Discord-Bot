package ca.allanwang.discord.bot.base

import com.gitlab.kordlib.kordx.emoji.DiscordEmoji
import dev.kord.core.entity.ReactionEmoji

/**
 * TODO remove after emoji package is updated
 */
fun DiscordEmoji.Generic.toReaction(): ReactionEmoji = ReactionEmoji.Unicode(unicode)
