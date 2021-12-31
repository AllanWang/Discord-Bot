package ca.allanwang.discord.bot.base

import ca.allanwang.discord.bot.core.withTimeout
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.modify.embed
import dev.kord.x.emoji.Emojis
import dev.kord.x.emoji.toReaction
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit

suspend fun Message.reactionAddEvents() = kord.events.filterIsInstance<ReactionAddEvent>()
    .filter { it.userId != kord.selfId }
    .filter { it.messageId == id }

suspend fun Message.confirmationReaction(user: Snowflake? = null): Boolean {
    val positive = Emojis.whiteCheckMark.toReaction()
    val negative = Emojis.negativeSquaredCrossMark.toReaction()
    addReaction(positive)
    addReaction(negative)
    return reactionAddEvents()
        .run { if (user == null) this else filter { it.userId == user } }
        .mapNotNull {
            when (it.emoji) {
                positive -> true
                negative -> false
                else -> null
            }
        }
        .withTimeout(TimeUnit.MINUTES.toMillis(1))
        .first()
}

suspend fun MessageChannelBehavior.paginatedMessage(
    descriptions: List<String>,
    addFooter: Boolean = true,
    template: EmbedBuilder.(description: String?) -> Unit = { description = it },
): Message {
    if (descriptions.size <= 1) {
        return createEmbed {
            template(descriptions.firstOrNull())
        }
    }

    val left = Emojis.arrowBackward.toReaction()
    val right = Emojis.arrowForward.toReaction()

    var currentIndex = 0

    fun EmbedBuilder.pageEmbed(index: Int) {
        template(descriptions[index])
        if (addFooter) {
            footer {
                text = "${index + 1}/${descriptions.size}"
            }
        }
    }

    val message = createEmbed {
        pageEmbed(0)
    }

    message.addReaction(left)
    message.addReaction(right)

    message.reactionAddEvents()
        .filter { it.emoji == left || it.emoji == right }
        .withTimeout(TimeUnit.MINUTES.toMillis(1))
        .collect {
            when (it.emoji) {
                left -> currentIndex--
                right -> currentIndex++
                else -> return@collect
            }
            currentIndex = Math.floorMod(currentIndex, descriptions.size)
            message.deleteReaction(it.userId, it.emoji)
            message.edit {
                embed {
                    pageEmbed(currentIndex)
                }
            }
        }

    return message
}
