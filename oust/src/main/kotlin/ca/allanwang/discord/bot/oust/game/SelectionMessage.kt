package ca.allanwang.discord.bot.oust.game

import ca.allanwang.discord.bot.base.appendBold
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.behavior.channel.MessageChannelBehavior
import com.gitlab.kordlib.core.behavior.channel.createMessage
import com.gitlab.kordlib.core.behavior.edit
import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.entity.ReactionEmoji
import com.gitlab.kordlib.core.event.message.ReactionAddEvent
import com.gitlab.kordlib.kordx.emoji.Emojis
import com.gitlab.kordlib.kordx.emoji.toReaction
import com.gitlab.kordlib.rest.builder.message.EmbedBuilder
import com.google.common.flogger.FluentLogger
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OustDiscordClient @Inject constructor(
    private val kord: Kord,
    private val channel: MessageChannelBehavior,
) : OustClient {

    override fun createEntry(player: OustPlayer, public: Boolean): OustClient.Entry = Entry(
        kord = kord, player = player, channel = channel
    )

    class Entry @Inject constructor(
        private val kord: Kord,
        private val player: OustPlayer,
        private val channel: MessageChannelBehavior
    ): OustClient.Entry {
        private val selectionMessage: SelectionMessage
            get() = _selectionMessage ?: createSelection().also { _selectionMessage = it }
        private var _selectionMessage: SelectionMessage? = null

        private fun createSelection(): SelectionMessage =
            SelectionMessage(kord = kord, player = player, channel = channel, header = {})

        override suspend fun selectCards(message: String, count: Int, cards: List<OustCard>): List<OustCard> {
            TODO("not implemented")
        }

        override suspend fun selectItem(message: String, items: List<String>): Int {
            TODO("not implemented")
        }

        override suspend fun sendMessage(message: String) {
            TODO("not implemented")
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

    suspend fun <T> selectAction(actions: List<T>, convert: (T) -> String): T {
        val index = selectAction(actions.map(convert))
        return actions[index]
    }

    suspend fun selectAction(actions: List<String>): Int {
        check(actions.isNotEmpty()) { "Cannot select actions from empty list" }
        val embedContent = EmbedBuilder().apply {
            header()
            field {
                name = "Actions"
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

        val index = kord.events.filterIsInstance<ReactionAddEvent>()
            .filter { it.messageId == message.id && it.user.id.value == player.info.id }
            .map { numberReactions.indexOf(it.emoji) }
            .filter { it in actions.indices }
            .first()
        return index
    }

}