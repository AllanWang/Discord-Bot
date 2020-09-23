package ca.allanwang.discord.bot.oust.game

import ca.allanwang.discord.bot.base.appendItalic
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.behavior.channel.MessageChannelBehavior
import com.google.common.flogger.FluentLogger
import java.awt.Color
import javax.inject.Inject

/**
 * Represents a single turn in the game.
 * Here, we have actions for one player, followed by a confirmation if necessary.
 */
interface OustTurn {

    val currentPlayer: OustPlayer

    sealed class Response<in T> {
        class Select<T>(val value: T) : Response<T>()
        object GoBack : Response<Any>()
    }

    /**
     * Allow the current player to choose from a list of actions.
     * [canGoBack] is true if a back button should be shown
     */
    suspend fun chooseAction(actions: List<OustAction>, canGoBack: Boolean): Response<OustAction>

    /**
     * Select a player from the allowed ids.
     * TODO() add enum for selection reason (money, kill)
     */
    suspend fun selectPlayers(allowed: Set<OustPlayer>): Response<OustPlayer>

    /**
     * Check if any user opposes the action.
     * Returns the first player to reject, or null if all players accept.
     */
    suspend fun checkRejection(message: String): OustPlayer?

    /**
     * Check with a specific user whether or not the action should be accepted
     */
    suspend fun checkIndividualRejection(player: OustPlayer): Boolean

    /**
     * Select card from hand to discard
     * TODO() not sure if this should return index or card
     */
    suspend fun discardCard(player: OustPlayer): OustCard

    /**
     * Given new cards, select a new hand with card count matching existing hand.
     */
    suspend fun selectNewHand(newCards: List<OustCard>): List<OustCard>

    suspend fun showNewHand(cards: List<OustCard>)

    /**
     * Show the final result of the turn.
     * TODO() add sealed class for all possible results
     */
    suspend fun showResult()

}

class OustClient @Inject constructor(
    private val kord: Kord,
    private val channel: MessageChannelBehavior
) {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()

        private val embedColor: Color = Color.decode("#DC1E28")
    }

    private var _selectionMessage: SelectionMessage? = null
    private val selectionMessage: SelectionMessage get() = _selectionMessage!!

    private fun newSelection(player: OustPlayer): SelectionMessage {
        return SelectionMessage(kord, player, channel, header = {
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
    }

    suspend fun chooseAction(player: OustPlayer, actions: List<OustAction>): OustAction {
        _selectionMessage = newSelection(player)
        val index = selectionMessage.selectAction(actions.map { it.name })
        return actions[index]
    }

    suspend fun confirmAction(players: List<OustPlayer>, message: String): Boolean {
        return true
    }
}