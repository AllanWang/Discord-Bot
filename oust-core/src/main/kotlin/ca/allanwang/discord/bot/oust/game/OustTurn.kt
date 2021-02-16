package ca.allanwang.discord.bot.oust.game

import com.google.common.flogger.FluentLogger

interface OustClient {
    interface Entry {
        suspend fun selectCards(message: String, count: Int, cards: List<OustCard>): List<OustCard>

        suspend fun selectItem(message: String, items: List<String>): Int

        suspend fun rebuttalAll(message: String, players: Set<OustPlayer>, card: OustCard): OustTurnRebuttal

        suspend fun rebuttal(
            message: String,
            player: OustPlayer,
            card: OustCard,
            vararg otherCards: OustCard
        ): OustTurnRebuttal

        suspend fun finalMessage(message: String)
    }

    suspend fun sendBroadcast(message: String)

    fun createEntry(player: OustPlayer, public: Boolean): Entry
}

/**
 * Represents a single turn in the game.
 * Here, we have actions for one player, followed by a confirmation if necessary.
 */
class OustTurn(
    val currentPlayer: OustPlayer,
    val otherPlayers: List<OustPlayer>,
    private val client: OustClient
) {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    interface Factory {
        fun get(currentPlayer: OustPlayer, otherPlayers: List<OustPlayer>): OustTurn
    }

    private val entry = client.createEntry(currentPlayer, public = true)

    private val OustPlayer.name: String get() = info.name

    suspend fun getStartRequest(): OustRequest {
        val actions = OustAction.values.filter { it.isPossible(currentPlayer) }
        // TODO check if we should have special case if only one action is possible
        return OustRequest.SelectAction(actions)
    }

    private suspend fun <T> OustClient.Entry.selectAction(
        message: String,
        actions: List<T>,
        convert: (T) -> String
    ): T {
        val index = selectItem(message, actions.map(convert))
        return actions[index]
    }

    suspend fun act(response: OustTurnResponse) {
        when (response) {
            is OustTurnResponse.SelectCardsShuffle -> {
            }
            is OustTurnResponse.Steal -> {
            }
            is OustTurnResponse.BigPayDay -> {
                currentPlayer.coins += 2
                entry.finalMessage("Increase coin by 2")
            }
            is OustTurnResponse.PayDay -> {
                currentPlayer.coins += 1
                entry.finalMessage("Increase coin by 1")
            }
            is OustTurnResponse.KillPlayer -> {
            }
        }
    }

    suspend fun handle(request: OustRequest, canGoBack: Boolean): OustResponse = when (request) {
        is OustRequest.SelectAction -> {
            val action = entry.selectAction("Select an action to perform", request.actions) { it.name }
            OustResponse.SelectedAction(action)
        }
        is OustRequest.SelectPlayerKill -> {
            val player = entry.selectAction("Select a player to ${request.type.name}", request.players) { it.name }
            OustResponse.TurnResponse(OustTurnResponse.KillPlayer(player, request.type))
        }
        is OustRequest.SelectPlayerSteal -> {
            val player = entry.selectAction("Select a player to steal from", request.players) { it.name }
            OustResponse.TurnResponse(OustTurnResponse.Steal(player))
        }
    }

    suspend fun rebuttal(response: OustTurnResponse): OustTurnRebuttal {
        fun allow(): OustTurnRebuttal = OustTurnRebuttal.Allow

        return when (response) {
            is OustTurnResponse.KillPlayer -> when (response.type) {
                KillType.Assassin -> entry.rebuttal(
                    "${currentPlayer.name} would like to assassinate ${response.player.name}",
                    response.player,
                    OustCard.BodyGuard
                )
                KillType.Oust -> allow()
            }
            is OustTurnResponse.PayDay -> allow()
            is OustTurnResponse.BigPayDay -> entry.rebuttalAll(
                "${currentPlayer.name} would like to collect 2 coins",
                otherPlayers.toSet(), OustCard.Banker
            )
            is OustTurnResponse.SelectCardsShuffle -> entry.rebuttalAll(
                "${currentPlayer.name} would like to shuffle cards from the deck",
                otherPlayers.toSet(),
                OustCard.Equalizer
            ) /* TODO verify role */
            is OustTurnResponse.Steal -> entry.rebuttal(
                "${currentPlayer.name} would like to steal from you",
                response.player,
                OustCard.Thief,
                OustCard.Equalizer /* TODO verify role */
            )
        }
    }
}
