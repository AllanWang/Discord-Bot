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

    override suspend fun getStartRequest(): OustRequest {
        return super.getStartRequest()
    }

    override suspend fun act(response: OustTurnResponse) {
        TODO("not implemented")
    }

    override suspend fun handle(request: OustRequest, canGoBack: Boolean): OustResponse {
        TODO("not implemented")
    }

    override suspend fun rebuttal(response: OustTurnResponse): OustTurnRebuttal {
        TODO("not implemented")
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

}

@Module(includes = [CoreModule::class])
object OustTurnModule {
    @Provides
    @OustScope
    fun turnFactory(kord: Kord, channel: MessageChannelBehavior): OustTurn.Factory = object : OustTurn.Factory {
        override fun get(currentPlayer: OustPlayer): OustTurn =
            OustTurnDiscord(kord = kord, channel = channel, currentPlayer = currentPlayer)
    }
}
