package ca.allanwang.discord.bot.oust.game

import com.gitlab.kordlib.common.entity.Snowflake
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.behavior.channel.MessageChannelBehavior
import com.gitlab.kordlib.core.behavior.channel.createEmbed
import com.gitlab.kordlib.core.entity.ReactionEmoji
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.event.message.ReactionAddEvent
import com.google.common.flogger.FluentLogger
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OustClient @Inject constructor(
    private val kord: Kord,
    private val channel: MessageChannelBehavior
) {

    companion object {

        private val logger = FluentLogger.forEnclosingClass()

        val numberReactions: List<ReactionEmoji> = listOf(
            "\u0031",
            "\u0032",
            "\u0033",
            "\u0034",
            "\u0035",
            "\u0036",
            "\u0037",
            "\u0038",
            "\u0039",
        ).map { ReactionEmoji.Unicode(it) }
    }

    private val messageCallbacks: MutableList<Callback> = mutableListOf()

    private class Callback(val action: MessageCreateEvent.() -> Boolean)

    suspend fun chooseAction(player: OustPlayer, actions: List<OustAction>): OustAction {
        val message = channel.createEmbed {
            title = "${player.info.name}'s Turn"
            description = buildString {
                append(if (player.cards.size == 1) "1 card" else "${player.cards.size} cards")
                append(" - ")
                append(if (player.coins == 1) "1 coin" else "${player.coins} coins")
            }
            actions.forEachIndexed { i, action ->
                field {
                    name = "${i + 1}: ${action.name}"
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