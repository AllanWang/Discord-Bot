package ca.allanwang.discord.bot.oust.game

import com.google.common.flogger.FluentLogger
import javax.inject.Inject
import javax.inject.Scope

@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class OustTurnScope

interface OustClient {
    interface Entry {
        suspend fun selectCards(message: String, count: Int, cards: List<OustCard>): List<OustCard>

        suspend fun selectItem(message: String, items: List<String>): Int

        suspend fun sendMessage(message: String)
    }

    fun createEntry(player: OustPlayer, public: Boolean): Entry
}

/**
 * Represents a single turn in the game.
 * Here, we have actions for one player, followed by a confirmation if necessary.
 */
class OustTurn @Inject constructor(
    val currentPlayer: OustPlayer,
    private val client: OustClient
) {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    interface Factory {
        fun get(currentPlayer: OustPlayer): OustTurn
    }

    sealed class Response<in T> {
        class Select<T>(val value: T) : Response<T>()
        object GoBack : Response<Any>()
    }

    suspend fun getStartRequest(): OustRequest {
        val actions = OustAction.values.filter { it.isPossible(currentPlayer) }
        // TODO check if we should have special case if only one action is possible
        return OustRequest.SelectAction(actions)
    }

    private suspend fun <T> OustClient.Entry.selectAction(message: String, actions: List<T>, convert: (T) -> String): T {
        val index = selectItem(message, actions.map(convert))
        return actions[index]
    }

    suspend fun act(response: OustTurnResponse) {
        TODO("not implemented")
    }

    private val entry = client.createEntry(currentPlayer, public = true)

     suspend fun handle(request: OustRequest, canGoBack: Boolean): OustResponse = when (request) {
        is OustRequest.SelectAction -> {
            val action = entry.selectAction("Select an action to perform", request.actions) { it.name }
            OustResponse.SelectedAction(action)
        }
        is OustRequest.SelectPlayerKill -> {
            val player = entry.selectAction("Select a player to ${request.type.name}", request.players) { it.info.name }
            OustResponse.TurnResponse(OustTurnResponse.KillPlayer(player, request.type))
        }
        is OustRequest.SelectPlayerSteal -> {
            val player = entry.selectAction("Select a player to steal from", request.players) { it.info.name }
            OustResponse.TurnResponse(OustTurnResponse.Steal(player))
        }
    }

     suspend fun rebuttal(response: OustTurnResponse): OustTurnRebuttal {
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
            is OustTurnResponse.SelectCardsShuffle -> rebuttalAll(OustCard.Equalizer) /* TODO verify role */
            is OustTurnResponse.Steal -> rebuttal(
                response.player,
                OustCard.Thief,
                OustCard.Equalizer /* TODO verify role */
            )
        }
    }

}