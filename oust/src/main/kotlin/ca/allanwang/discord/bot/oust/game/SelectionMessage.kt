package ca.allanwang.discord.bot.oust.game

import ca.allanwang.discord.bot.base.appendBold
import ca.allanwang.discord.bot.base.appendItalic
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
import java.awt.Color
import javax.inject.Inject

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
            .filter { it.messageId == message.id && it.user.id == player.info.id }
            .map { numberReactions.indexOf(it.emoji) }
            .filter { it in actions.indices }
            .first()
        return index
    }

}