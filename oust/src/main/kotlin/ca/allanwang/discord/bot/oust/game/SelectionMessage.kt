package ca.allanwang.discord.bot.oust.game

import ca.allanwang.discord.bot.base.appendBold
import ca.allanwang.discord.bot.base.appendItalic
import ca.allanwang.discord.bot.base.appendPlural
import ca.allanwang.discord.bot.base.toReaction
import com.gitlab.kordlib.kordx.emoji.Emojis
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.event.Event
import dev.kord.core.event.message.ReactionAddEvent
import dev.kord.core.event.message.ReactionRemoveEvent
import dev.kord.rest.builder.message.EmbedBuilder
import com.google.common.flogger.FluentLogger
import dev.kord.common.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@OustScope
class OustDiscordClient @Inject constructor(
    private val kord: Kord,
    private val channel: MessageChannelBehavior,
) : OustClient {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()

        private val embedColor: Color = Color(0xFFDC1E28.toInt())
    }

    override fun createEntry(player: OustPlayer, public: Boolean): OustClient.Entry = Entry(player = player)

    override suspend fun sendBroadcast(message: String) {
        channel.createEmbed {
            color = embedColor
            title = "Oust"
            description = message
        }
    }

    private val OustPlayer.snowflake: Snowflake get() = Snowflake(info.id)

    private inner class Entry(
        private val player: OustPlayer
    ) : OustClient.Entry {

        private val selectionMessage: SelectionMessage = selectionMessage {
            title = "${player.info.name}'s Turn"
            field {
                name = "Items"
                value = buildString {
                    appendPlural(player.cards.size, "Card")
                    append(" - ")
                    appendItalic {
                        append(player.cards.joinToString(" • ") {
                            if (it.visible) it.value.name else "Unknown"
                        })
                    }
                    appendLine()
                    appendPlural(player.coins, "Coin")
                }
            }
        }

        private inline fun selectionMessage(crossinline header: EmbedBuilder.() -> Unit): SelectionMessage =
            SelectionMessage(kord = kord, channel = channel, header = {
                color = embedColor
                header()
            })

        override suspend fun selectCards(message: String, count: Int, cards: List<OustCard>): List<OustCard> {
            return selectionMessage.selectActions(player.snowflake, cards.map { it.name }, count) {
                field {
                    name = "Select Cards"
                    value = message
                }
            }.map { cards[it] }
        }

        override suspend fun selectItem(message: String, items: List<String>): Int {
            return selectionMessage.selectAction(player.snowflake, items) {
                field {
                    name = "Select Action"
                    value = message
                }
            }
        }

        override suspend fun rebuttalAll(message: String, players: Set<OustPlayer>, card: OustCard): OustTurnRebuttal {
            val selectionMessage = selectionMessage {
                title = "Rebuttal (${player.info.name})"
                description = buildString {
                    append(message)
                    append("\n\n")
                    append("Rebuttal ends when one person declines or when everyone accepts")
                }
            }
            val idMap = players.associateBy { it.snowflake }
            val user =
                selectionMessage.veto(idMap.keys, acceptText = "Accept", rejectText = "Decline with ${card.name}")
            return if (user == null) OustTurnRebuttal.Allow else OustTurnRebuttal.Decline(idMap.getValue(user), card)
        }

        override suspend fun rebuttal(
            message: String,
            player: OustPlayer,
            card: OustCard,
            vararg otherCards: OustCard
        ): OustTurnRebuttal {
            val selectionMessage = selectionMessage {
                title = "Rebuttal (${player.info.name})"
                description = message
            }
            val cards = listOf(card, *otherCards)
            val cardActions = cards.map { "Decline with ${it.name}" }
            val actions = listOf("Accept", *cardActions.toTypedArray())
            val index = selectionMessage.selectAction(player.snowflake, actions)
            if (index == 0) return OustTurnRebuttal.Allow
            return OustTurnRebuttal.Decline(player, cards[index - 1])
        }

        override suspend fun finalMessage(message: String) {
            channel.createEmbed {
                color = embedColor
                description = message
            }
        }
    }
}

class SelectionMessage @Inject constructor(
    private val kord: Kord,
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

    private suspend fun sendMessage(
        title: String,
        actions: List<String>,
        embedBuilder: EmbedBuilder.() -> Unit
    ): Message {

        val embedContent: EmbedBuilder.() -> Unit = {
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
            _message?.let { it.edit { embed(embedContent) } }
                ?: channel.createMessage { embed(embedContent) }
        _message = message

        // Given that list size is at most 10, it's fine to avoid converting to set for checking elements

        val currentReactions = message.reactions.map { it.emoji }.distinct()

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

    private object PartialCollector {
        class PartialCollectionException(message: String? = null) :
            CancellationException(message ?: "Finished partial collection")

        fun finishCollecting(message: String? = null): Nothing = throw PartialCollectionException(message)
    }

    private suspend inline fun <T> Flow<T>.collectPartial(crossinline action: suspend PartialCollector.(value: T) -> Unit) {
        try {
            collect { PartialCollector.action(it) }
        } catch (e: PartialCollector.PartialCollectionException) {
            // ignore
        }
    }

    suspend fun veto(
        userIds: Set<Snowflake>,
        acceptText: String = "Accept",
        rejectText: String = "Reject",
        embedBuilder: EmbedBuilder.() -> Unit = {}
    ): Snowflake? {
        check(userIds.isNotEmpty()) { "Cannot veto with empty user list" }
        val actions = listOf(acceptText, rejectText)
        val message = sendMessage(title = "Actions", actions = actions, embedBuilder = embedBuilder)

        val acceptingUsers: MutableSet<Snowflake> = mutableSetOf()
        var rejectingUser: Snowflake? = null

        kord.events.mapNotNull { it.convert(userIds, actions.indices) }.collectPartial {
            if (it.index == 1 && it.add) {
                rejectingUser = it.userId
                finishCollecting("Rejected user")
            }
            if (it.index == 0) {
                if (it.add) acceptingUsers.add(it.userId)
                else acceptingUsers.remove(it.userId)
            }
            // todo ("Reset this")
            if (acceptingUsers.size >= 1) finishCollecting("Done all collection")
//            if (acceptingUsers.size == userIds.size) finishCollecting("Done all collection")
        }

        return rejectingUser
    }

    /**
     * Allow [userId] to select a single action from [actions]. This is an optimized version of [selectActions],
     * and returns as soon as one reaction is selected.
     */
    suspend fun selectAction(
        userId: Snowflake,
        actions: List<String>,
        embedBuilder: EmbedBuilder.() -> Unit = {}
    ): Int {
        check(actions.isNotEmpty()) { "Cannot select actions from empty list" }
        val message = sendMessage(title = "Actions", actions = actions, embedBuilder = embedBuilder)
        val userIds = setOf(userId)
        val index = kord.events.mapNotNull { it.convert(userIds, actions.indices) }
            .first { it.add }.index
        logger.atInfo().log("Selected action %d", index)

        message.deleteReaction(userId, numberReactions[index])

        return index
    }

    private data class ReactionEvent(val add: Boolean, val index: Int, val userId: Snowflake)

    private suspend fun Event.convert(userIds: Set<Snowflake>, validIndices: IntRange): ReactionEvent? {
        when (this) {
            is ReactionAddEvent -> {
                if (messageId != message.id) return null
                if (userId == kord.selfId) return null
                val index = numberReactions.indexOf(emoji)
                if (index !in validIndices) return null
                if (userId !in userIds) {
                    // Delete if not from appropriate user
                    message.deleteReaction(userId, emoji)
                    return null
                }
                return ReactionEvent(add = true, index = index, userId = userId)
            }
            is ReactionRemoveEvent -> {
                if (messageId != message.id) return null
                if (userId == kord.selfId) return null
                val index = numberReactions.indexOf(emoji)
                if (index !in validIndices) return null
                if (userId !in userIds) return null
                return ReactionEvent(add = false, index = index, userId = userId)
            }
            else -> return null
        }
    }

    /**
     * Allows [userId] to select [count] entries from [actions].
     *
     * Returns a list of selected indices.
     */
    suspend fun selectActions(
        userId: Snowflake,
        actions: List<String>,
        count: Int,
        embedBuilder: EmbedBuilder.() -> Unit = {}
    ): List<Int> {
        check(actions.size >= count) { "Cannot select $count actions from ${actions.size} sized list" }
        val message = sendMessage(title = "Actions", actions = actions, embedBuilder = embedBuilder)

        val selection: MutableSet<Int> = mutableSetOf()

        val userIds = setOf(userId)

        kord.events.mapNotNull { it.convert(userIds, actions.indices) }.collectPartial {
            if (it.add) selection.add(it.index)
            else selection.remove(it.index)
            if (selection.size == count) finishCollecting("Done collecting items")
        }

        val sorted = selection.sorted()

        sorted.forEach { message.deleteReaction(userId, numberReactions[it]) }

        logger.atInfo().log("Selected actions %s", sorted)
        return sorted
    }

    suspend fun removeReactions() {
        val message = _message ?: return
        message.deleteAllReactions()
    }

}