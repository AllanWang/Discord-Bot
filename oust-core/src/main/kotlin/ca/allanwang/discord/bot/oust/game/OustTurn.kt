package ca.allanwang.discord.bot.oust.game

import javax.inject.Scope

@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class OustTurnScope

/**
 * Represents a single turn in the game.
 * Here, we have actions for one player, followed by a confirmation if necessary.
 */
interface OustTurn {

    interface Factory {
        fun get(currentPlayer: OustPlayer): OustTurn
    }

    val currentPlayer: OustPlayer

    sealed class Response<in T> {
        class Select<T>(val value: T) : Response<T>()
        object GoBack : Response<Any>()
    }

    suspend fun getStartRequest(): OustRequest {
        val actions = OustAction.values.filter { it.isPossible(currentPlayer) }
        // TODO check if we should have special case if only one action is possible
        return OustRequest.SelectAction(actions)
    }

    suspend fun rebuttal(response: OustTurnResponse): OustTurnRebuttal

    suspend fun act(response: OustTurnResponse)

    suspend fun handle(request: OustRequest, canGoBack: Boolean): OustResponse

}