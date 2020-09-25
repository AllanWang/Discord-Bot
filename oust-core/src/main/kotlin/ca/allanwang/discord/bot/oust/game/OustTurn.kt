package ca.allanwang.discord.bot.oust.game

import com.google.common.flogger.FluentLogger
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import java.awt.Color
import javax.inject.Inject
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

    suspend fun handle(request: OustRequest, canGoBack: Boolean): OustResponse

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