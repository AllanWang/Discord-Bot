package ca.allanwang.discord.bot.oust.game

import ca.allanwang.discord.bot.base.appendBold
import ca.allanwang.discord.bot.base.appendItalic
import com.gitlab.kordlib.common.entity.Snowflake
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.behavior.channel.MessageChannelBehavior
import com.gitlab.kordlib.core.behavior.channel.createEmbed
import com.gitlab.kordlib.core.behavior.channel.createMessage
import com.gitlab.kordlib.core.behavior.edit
import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.entity.ReactionEmoji
import com.gitlab.kordlib.core.event.Event
import com.gitlab.kordlib.core.event.message.ReactionAddEvent
import com.gitlab.kordlib.core.event.message.ReactionRemoveEvent
import com.gitlab.kordlib.kordx.emoji.Emojis
import com.gitlab.kordlib.kordx.emoji.toReaction
import com.gitlab.kordlib.rest.builder.message.EmbedBuilder
import com.google.common.flogger.FluentLogger
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import java.awt.Color
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class OustDiscordClient @Inject constructor(
    private val kord: Kord,
    private val channel: MessageChannelBehavior,
) : OustClient {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()

        private val embedColor: Color = Color.decode("#DC1E28")
    }

    override fun createEntry(player: OustPlayer, public: Boolean): OustClient.Entry = Entry(player = player)

    private inner class Entry(
        private val player: OustPlayer
    ) : OustClient.Entry {

        private val selectionMessage: SelectionMessage =
            SelectionMessage(kord = kord, player = player, channel = channel, header = {
                color = embedColor
                title = "${player.info.name}'s Turn"
                field {
                    name = "Items"
                    value = buildString {
                        append(if (player.cards.size == 1) "1 Card" else "${player.cards.size} Cards")
                        append(" - ")
                        appendItalic {
                            append(player.cards.joinToString(" • ") {
                                if (it.visible) it.value.name else "Unknown"
                            })
                        }
                        appendLine()
                        append(if (player.coins == 1) "1 Coin" else "${player.coins} Coins")
                    }
                }
            })

        override suspend fun selectCards(message: String, count: Int, cards: List<OustCard>): List<OustCard> {
            return selectionMessage.selectActions(cards.map { it.name }, count) {
                field {
                    name = "Select Cards"
                    value = message
                }
            }.map { cards[it] }
        }

        override suspend fun selectItem(message: String, items: List<String>): Int {
            return selectionMessage.selectAction(items) {
                field {
                    name = "Select Items"
                    value = message
                }
            }
        }

        override suspend fun sendMessage(message: String) {
            channel.createEmbed {
                color = embedColor
                description = message
            }
        }
    }
}

class SelectionMessage @Inject constructor(
    private val kord: Kord,
    private val player: OustPlayer,
    private val channel: MessageChannelBehavior,
    private val header: EmbedBuilder.() -> Unit
) {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()

        val numberReactions: List<ReactionEmoji> = listOf(
            Emojis.one,
            Emojis.two,
            Emojis.three,
            Emojis.four,
            Emojis.five,
            Emojis.six,
            Emojis.seven,
            Emojis.eight,
            Emojis.nine,
        ).map { it.toReaction() }
    }

    private var _message: Message? = null

    private val OustPlayer.snowflake: Snowflake get() = Snowflake(info.id)

    private suspend fun sendMessage(
        title: String,
        actions: List<String>,
        embedBuilder: EmbedBuilder.() -> Unit
    ): Message {
        val embedContent = EmbedBuilder().apply {
            header()
            embedBuilder()
            field {
                name = title
                value = buildString {
                    actions.forEachIndexed { i, action ->
                        appendBold {
                            append(i + 1)
                        }
                        append(": ")
                        append(action)
                        appendLine()
                    }
                }
            }
        }

        val message =
            _message?.let { it.edit { embed = embedContent } }
                ?: channel.createMessage { embed = embedContent }
        _message = message

        // Given that list size is at most 10, it's fine to avoid converting to set for checking elements

        val currentReactions = message.reactions.map { it.emoji }

        val actionsToAdd = numberReactions.subList(0, actions.size)
        actionsToAdd.filter { it !in currentReactions }.forEach {
            message.addReaction(it)
        }
        val actionsToRemove = numberReactions.subList(actions.size, numberReactions.size)
        currentReactions.filter { it in actionsToRemove }.forEach {
            message.deleteReaction(it)
        }

        return message
    }

    suspend fun selectAction(actions: List<String>, embedBuilder: EmbedBuilder.() -> Unit = {}): Int {
        check(actions.isNotEmpty()) { "Cannot select actions from empty list" }
        val message = sendMessage(title = "Actions", actions = actions, embedBuilder = embedBuilder)

        val index = kord.events.mapNotNull { it.convert(actions.indices) }
            .first { it.add }.index
        logger.atInfo().log("Selected action %d", index)

        message.deleteReaction(player.snowflake, numberReactions[index])

        return index
    }

    private data class ReactionEvent(val add: Boolean, val index: Int)

    private suspend fun Event.convert(validIndices: IntRange): ReactionEvent? {
        when (this) {
            is ReactionAddEvent -> {
                if (messageId != message.id) return null
                if (userId == kord.selfId) return null
                val index = numberReactions.indexOf(emoji)
                if (index !in validIndices) return null
                if (userId.value != player.info.id) {
                    // Delete if not from appropriate user
                    message.deleteReaction(userId, emoji)
                    return null
                }
                return ReactionEvent(add = true, index = index)
            }
            is ReactionRemoveEvent -> {
                if (messageId != message.id) return null
                if (userId == kord.selfId) return null
                val index = numberReactions.indexOf(emoji)
                if (index !in validIndices) return null
                if (userId.value != player.info.id) return null
                return ReactionEvent(add = false, index = index)
            }
            else -> return null
        }
    }

    suspend fun selectActions(
        actions: List<String>,
        count: Int,
        embedBuilder: EmbedBuilder.() -> Unit = {}
    ): List<Int> {
        check(actions.size >= count) { "Cannot select $count actions from ${actions.size} sized list" }
        val message = sendMessage(title = "Actions", actions = actions, embedBuilder = embedBuilder)

        val selection: MutableSet<Int> = mutableSetOf()

        kord.events.mapNotNull { it.convert(actions.indices) }.collect {
            if (it.add) selection.add(it.index)
            else selection.remove(it.index)
            if (selection.size == count) throw CancellationException("Done collecting items")
        }

        val sorted = selection.sorted()

        sorted.forEach { message.deleteReaction(player.snowflake, numberReactions[it]) }

        logger.atInfo().log("Selected actions %s", sorted)
        return sorted
    }

}