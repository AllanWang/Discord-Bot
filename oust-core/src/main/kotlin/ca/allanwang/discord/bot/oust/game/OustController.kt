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

    }

    suspend fun turn() {
        val response = runTurn()
    }

    suspend fun runTurn(): OustTurnResponse {
        val turn = turnFactory.get(game.currentPlayer)
        val requests: LinkedList<OustRequest> = LinkedList()

        var request: OustRequest = turn.getStartRequest()
        while (true) {
            requests.add(request)
            val response = turn.handle(request, canGoBack = requests.size > 1)
            request = when (val step = getNextRequest(response)) {
                is TurnStep.GoBack -> {
                    requests.removeLast()
                    requests.last
                }
                is TurnStep.NextRequest -> step.request
                is TurnStep.TurnResponse -> return step.response
            }
        }
    }

    private fun getNextRequest(response: OustResponse): TurnStep = when (response) {
        is OustResponse.GoBack -> TurnStep.GoBack
        is OustResponse.TurnResponse -> TurnStep.TurnResponse(response.response)
        else -> TODO()
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