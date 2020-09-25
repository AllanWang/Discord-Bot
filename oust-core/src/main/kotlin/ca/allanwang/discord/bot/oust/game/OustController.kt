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

    private var requests: LinkedList<OustRequest> = LinkedList()
    private var turn: OustTurn = turnFactory.get(game.currentPlayer)

    suspend fun test() {
        val action = chooseAction()
        logger.atInfo().log("Action %s", action)
    }

    suspend fun turnResult(): OustTurnResponse {
        requests = LinkedList()
        val firstRequest = OustRequest.SelectAction(emptyList())
        val firstResponse = turn.handle(firstRequest, canGoBack = false)
        var step = getNextRequest(firstResponse)
        while (true) {
            when (step) {
                is TurnStep.NextRequest -> {
                    requests.add(step.request)
                    val response = turn.handle(step.request, canGoBack = true)
                    step = getNextRequest(response)
                }
                is TurnStep.TurnResponse -> return step.response
            }
        }
    }

    private fun getNextRequest(response: OustResponse): TurnStep = when (response) {
        is OustResponse.GoBack -> {
            // TODO verify that only one pop is needed
            val oldRequest = requests.pop()
            TurnStep.NextRequest(oldRequest)
        }
        is OustResponse.TurnResponse -> TurnStep.TurnResponse(response.response)
    }

    private sealed class TurnStep {
        class NextRequest(val request: OustRequest) : TurnStep()
        class TurnResponse(val response: OustTurnResponse) : TurnStep()
    }

    suspend fun chooseAction(): OustTurn.Response<OustAction> {
        val validActions = game.validActions()
        if (validActions.size == 1) {
            // message forced action?
            return OustTurn.Response.Select(validActions.first())
        }
        return handle(turn.chooseAction(validActions, false))
    }

    private suspend fun handle(response: OustTurn.Response<OustResponse>): OustTurn.Response<OustResponse> {
        TODO()
    }

}

@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class OustScope