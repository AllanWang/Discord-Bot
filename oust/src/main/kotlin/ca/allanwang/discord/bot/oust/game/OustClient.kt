package ca.allanwang.discord.bot.oust.game

import ca.allanwang.discord.bot.base.appendBold
import ca.allanwang.discord.bot.base.appendItalic
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.behavior.channel.MessageChannelBehavior
import com.gitlab.kordlib.core.behavior.channel.createEmbed
import com.gitlab.kordlib.core.entity.ReactionEmoji
import com.gitlab.kordlib.core.event.message.ReactionAddEvent
import com.gitlab.kordlib.kordx.emoji.Emojis
import com.gitlab.kordlib.kordx.emoji.toReaction
import com.google.common.flogger.FluentLogger
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.awt.Color
import javax.inject.Inject

class OustClient @Inject constructor(
    private val kord: Kord,
    private val channel: MessageChannelBehavior
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

        private val embedColor: Color = Color.decode("#DC1E28")
    }

    suspend fun chooseAction(player: OustPlayer, actions: List<OustAction>): OustAction {
        val message = channel.createEmbed {
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
            field {
                name = "Actions"
                value = buildString {
                    actions.forEachIndexed { i, action ->
                        appendBold {
                            append(i + 1)
                        }
                        append(": ")
                        append(action.name)
                        appendLine()
                    }
                }
            }
        }
        (actions.indices).forEach {
            message.addReaction(numberReactions[it])
        }
        val index = kord.events.filterIsInstance<ReactionAddEvent>()
            .filter { it.messageId == message.id && it.user.id == player.info.id }
            .map { numberReactions.indexOf(it.emoji) }
            .filter { it in actions.indices }
            .first()
        return actions[index]
    }
}