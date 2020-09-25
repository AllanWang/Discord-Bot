package ca.allanwang.discord.bot.oust.game

import ca.allanwang.discord.bot.base.appendItalic
import ca.allanwang.discord.bot.core.CoreModule
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.behavior.channel.MessageChannelBehavior
import com.google.common.flogger.FluentLogger
import dagger.Module
import dagger.Provides
import java.awt.Color

class OustTurnDiscord(
    override val currentPlayer: OustPlayer,
    private val kord: Kord,
    private val channel: MessageChannelBehavior
) : OustTurn {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()

        private val embedColor: Color = Color.decode("#DC1E28")
    }

    private var _selectionMessage: SelectionMessage? = null
    private val selectionMessage: SelectionMessage
        get() = _selectionMessage ?: newSelection(currentPlayer)
            .also { _selectionMessage = it }

    override suspend fun act(response: OustTurnResponse) {
        TODO("not implemented")
    }

    override suspend fun handle(request: OustRequest, canGoBack: Boolean): OustResponse = when (request) {
        is OustRequest.SelectAction -> {
            val action = selectionMessage.selectAction(request.actions) { it.name }
            OustResponse.SelectedAction(action)
        }
        is OustRequest.SelectPlayerKill -> {
            val player = selectionMessage.selectAction(request.players) { it.info.name }
            OustResponse.TurnResponse(OustTurnResponse.KillPlayer(player, request.type))
        }
        is OustRequest.SelectCardsShuffle -> {
            TODO()
        }
        is OustRequest.SelectPlayerSteal -> {
            val player = selectionMessage.selectAction(request.players) { it.info.name }
            OustResponse.TurnResponse(OustTurnResponse.Steal(player))
        }
    }

    override suspend fun rebuttal(response: OustTurnResponse): OustTurnRebuttal {
        suspend fun rebuttalAll(card: OustCard): OustTurnRebuttal {
            TODO()
        }

        suspend fun rebuttal(player: OustPlayer, card: OustCard, vararg otherCards: OustCard): OustTurnRebuttal {
            TODO()
        }

        fun allow(): OustTurnRebuttal = OustTurnRebuttal.Allow

        return when (response) {
            is OustTurnResponse.KillPlayer -> when (response.type) {
                KillType.Assassin -> rebuttal(response.player, OustCard.BodyGuard)
                KillType.Oust -> allow()
            }
            is OustTurnResponse.PayDay -> allow()
            is OustTurnResponse.BigPayDay -> rebuttalAll(OustCard.Banker)
            is OustTurnResponse.Steal -> rebuttal(
                response.player,
                OustCard.Thief,
                OustCard.Equalizer /* TODO verify role */
            )
        }
    }

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

}

@Module(includes = [CoreModule::class])
object OustTurnModule {
    @Provides
    @OustScope
    fun turnFactory(
        kord: Kord,
        channel: MessageChannelBehavior
    ): OustTurn.Factory = object : OustTurn.Factory {
        override fun get(currentPlayer: OustPlayer): OustTurn =
            OustTurnDiscord(
                kord = kord,
                channel = channel,
                currentPlayer = currentPlayer
            )
    }
}
