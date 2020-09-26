package ca.allanwang.discord.bot.oust.game

import ca.allanwang.discord.bot.base.appendBold
import ca.allanwang.discord.bot.base.appendItalic
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
import kotlinx.coroutines.flow.*
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

    private inner class Entry @Inject constructor(
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
           return  selectionMessage.selectAction(items) {
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

    private var actions: List<String> = emptyList()
    private lateinit var message: Message

    private suspend fun sendMessage(title: String, actions: List<String>, embedBuilder: EmbedBuilder.() -> Unit) {
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

        if (this.actions.isEmpty()) {
            message = channel.createMessage { embed = embedContent }
        } else {
            message.edit { embed = embedContent }
        }
        this.actions = actions

        (actions.indices).forEach {
            message.addReaction(numberReactions[it])
        }
    }

    suspend fun selectAction(actions: List<String>, embedBuilder: EmbedBuilder.() -> Unit = {}): Int {
        check(actions.isNotEmpty()) { "Cannot select actions from empty list" }
        sendMessage(title = "Actions", actions = actions, embedBuilder = embedBuilder)

        val index = kord.events.filterIsInstance<ReactionAddEvent>()
            .filter { it.messageId == message.id && it.user.id.value == player.info.id }
            .map { numberReactions.indexOf(it.emoji) }
            .filter { it in actions.indices }
            .first()
        logger.atInfo().log("Selected action %d", index)
        return index
    }

    private data class ReactionEvent(val add: Boolean, val index: Int)

    suspend fun selectActions(actions: List<String>, count: Int, embedBuilder: EmbedBuilder.() -> Unit = {}): List<Int> {
        check(actions.size >= count) { "Cannot select $count actions from ${actions.size} sized list" }
        sendMessage(title = "Actions", actions = actions, embedBuilder = embedBuilder)

        val selection: MutableSet<Int> = mutableSetOf()

        fun convert(event: Event): ReactionEvent? {
            when (event) {
                is ReactionAddEvent -> {
                    if (event.messageId != message.id || event.user.id.value != player.info.id) return null
                    val index = numberReactions.indexOf(event.emoji)
                    if (index !in actions.indices) return null
                    return ReactionEvent(add = true, index = index)
                }
                is ReactionRemoveEvent -> {
                    if (event.messageId != message.id || event.user.id.value != player.info.id) return null
                    val index = numberReactions.indexOf(event.emoji)
                    if (index !in actions.indices) return null
                    return ReactionEvent(add = false, index = index)
                }
                else -> return null
            }
        }


        kord.events.mapNotNull { convert(it) }.collect {
            if (it.add) selection.add(it.index)
            else selection.remove(it.index)
            if (selection.size == count) throw CancellationException("Done collecting items")
        }

        val sorted = selection.sorted()

        logger.atInfo().log("Selected actions %s", sorted)
        return sorted
    }

}