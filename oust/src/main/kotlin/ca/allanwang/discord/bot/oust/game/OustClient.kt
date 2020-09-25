package ca.allanwang.discord.bot.oust.game

import ca.allanwang.discord.bot.base.appendItalic
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.behavior.channel.MessageChannelBehavior
import com.google.common.flogger.FluentLogger
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import java.awt.Color
import javax.inject.Inject


class OustTurnDiscord(
    override val currentPlayer: OustPlayer
) : OustTurn {

    override suspend fun chooseAction(actions: List<OustAction>, canGoBack: Boolean): OustTurn.Response<OustAction> {
        TODO("not implemented")
    }

    override suspend fun selectPlayers(allowed: Set<OustPlayer>): OustTurn.Response<OustPlayer> {
        TODO("not implemented")
    }

    override suspend fun checkRejection(message: String): OustPlayer? {
        TODO("not implemented")
    }

    override suspend fun checkIndividualRejection(player: OustPlayer): Boolean {
        TODO("not implemented")
    }

    override suspend fun discardCard(player: OustPlayer): OustCard {
        TODO("not implemented")
    }

    override suspend fun selectNewHand(newCards: List<OustCard>): List<OustCard> {
        TODO("not implemented")
    }

    override suspend fun showNewHand(cards: List<OustCard>) {
        TODO("not implemented")
    }

    override suspend fun showResult() {
        TODO("not implemented")
    }

}

@Module
object OustTurnModule {
    @Provides
    @OustScope
    fun turnFactory(): OustTurn.Factory = object : OustTurn.Factory {
        override fun get(currentPlayer: OustPlayer): OustTurn = OustTurnDiscord(currentPlayer)
    }
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