package ca.allanwang.discord.bot.oust.game

import com.google.common.flogger.FluentLogger
import java.util.*
import javax.inject.Inject
import javax.inject.Scope

@OustScope
class OustController @Inject constructor(
    private val game: OustGame,
    private val turnFactory: OustTurn.Factory
) {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    suspend fun test() {
        turn()
    }

    suspend fun turnLoop() {
        var finishedGame: FinishedGame?
        do {
            turn()
            finishedGame = finishedGameState()
        } while (finishedGame == null)
        TODO("Print ending")
    }

    suspend fun turn() {
        val turn = turnFactory.get(game.currentPlayer)
        val response = runTurn(turn)
        when (val rebuttal = turn.rebuttal(response)) {
            is OustTurnRebuttal.Allow -> turn.act(response)
            is OustTurnRebuttal.Decline -> TODO()
        }
        game.currentPlayerIndex = (game.currentPlayerIndex + 1) % game.players.size
    }

    data class FinishedGame(val winner: OustPlayer)

    private suspend fun finishedGameState(): FinishedGame? {
        val remainingPlayers = game.players.filter { it.cards.isNotEmpty() }
        if (remainingPlayers.isEmpty()) throw IllegalArgumentException("Game has no remaining players")
        if (remainingPlayers.size > 1) return null
        return FinishedGame(remainingPlayers.first())
    }

    private suspend fun isGameFinished(): Boolean = game.players.filter { it.cards.isNotEmpty() }.size <= 1

    suspend fun runTurn(turn: OustTurn): OustTurnResponse {
        val requests: LinkedList<OustRequest> = LinkedList()

        var request: OustRequest = turn.getStartRequest()
        while (true) {
            requests.add(request)
            val response = turn.handle(request, canGoBack = requests.size > 1)
            request = when (val step = getNextRequest(turn, response)) {
                is TurnStep.GoBack -> {
                    requests.removeLast()
                    requests.last
                }
                is TurnStep.NextRequest -> step.request
                is TurnStep.TurnResponse -> return step.response
            }
        }
    }

    private fun OustTurn.otherPlayers() = game.players.filter { it !== currentPlayer && it.cards.isNotEmpty() }

    private fun getNextRequest(turn: OustTurn, response: OustResponse): TurnStep {
        fun next(request: OustRequest) = TurnStep.NextRequest(request)
        fun response(response: OustTurnResponse) = TurnStep.TurnResponse(response)

        return when (response) {
            is OustResponse.GoBack -> TurnStep.GoBack
            is OustResponse.SelectedAction -> when (response.action) {
                OustAction.Oust -> next(
                    OustRequest.SelectPlayerKill(
                        players = turn.otherPlayers(), type = KillType.Oust
                    )
                )
                OustAction.Assassinate -> next(
                    OustRequest.SelectPlayerKill(
                        players = turn.otherPlayers(), type = KillType.Assassin
                    )
                )
                OustAction.Shuffle -> next(
                    OustRequest.SelectCardsShuffle(deckCards = game.deck.take(2))
                )
                OustAction.PayDay -> response(OustTurnResponse.PayDay)
                OustAction.BigPayDay -> response(OustTurnResponse.BigPayDay)
                OustAction.Steal -> next(OustRequest.SelectPlayerSteal(turn.otherPlayers()))
            }
            is OustResponse.TurnResponse -> response(response.response)
        }
    }

    private sealed class TurnStep {
        object GoBack : TurnStep()
        class NextRequest(val request: OustRequest) : TurnStep()
        class TurnResponse(val response: OustTurnResponse) : TurnStep()
    }

}

@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class OustScope