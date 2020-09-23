package ca.allanwang.discord.bot.oust.game

import com.google.common.flogger.FluentLogger
import javax.inject.Inject

class OustController @Inject constructor(
    val game: OustGame,
    val client: OustClient
) {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    // private var turn: OustTurn

    suspend fun test() {
        val action = chooseAction()
        logger.atInfo().log("Action %s", action)
    }

    suspend fun chooseAction(): OustAction {
        val validActions = game.validActions()
        if (validActions.size == 1) {
            // message forced action?
            return validActions.first()
        }
        return client.chooseAction(game.currentPlayer, validActions)
    }

}
