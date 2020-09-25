package ca.allanwang.discord.bot.oust.game

import kotlin.math.min

sealed class OustMove {

    abstract fun OustGame.apply()

    protected operator fun List<OustPlayer>.get(id: String): OustPlayer = firstOrNull { it.info.id == id }
        ?: throw IllegalArgumentException("Could not find player with matching id")

    protected fun OustGame.discardCard(id: String, cardIndex: Int) {
        val player = players[id]
        player.cards.removeAt(cardIndex)
    }

    data class Oust(val player: String, val cardIndex: Int) : OustMove() {
        override fun OustGame.apply() {
            currentPlayer.spendCoins(OustGame.OUST_COST)
            discardCard(player, cardIndex)
        }
    }

    data class Assassinate(val player: String, val cardIndex: Int) : OustMove() {
        override fun OustGame.apply() {
            currentPlayer.spendCoins(OustGame.ASSASSINATION_COST)
            discardCard(player, cardIndex)
        }
    }

    data class Steal(val player: String) : OustMove() {
        override fun OustGame.apply() {
            val otherPlayer = players[player]
            val value = min(otherPlayer.coins, OustGame.STEAL_AMOUNT)
            otherPlayer.coins -= value
            currentPlayer.coins += value
        }
    }

}

fun OustGame.validActions(): List<OustAction> {
    val currentCoins = currentPlayer.coins
    if (currentCoins >= OustGame.FORCE_OUST_THRESHOLD) return listOf(OustAction.Oust)
    return OustAction.values.filter { currentCoins >= it.requiredCoins }
}

fun OustGame.act(action: OustAction) {
    
}

enum class OustAction(val requiredCoins: Int = 0, val blockable: Boolean = true) {
    Oust(requiredCoins = OustGame.OUST_COST, blockable = false),
    Assassinate(requiredCoins = OustGame.ASSASSINATION_COST),
    Steal,
    PayDay(blockable = false),
    BigPayDay,
    Equalize,
    Shuffle;

    fun OustGame.isPossible() : Boolean{
        val currentCoins = currentPlayer.coins
        if (this@OustAction == Oust) return currentCoins >= OustGame.OUST_COST
        if (currentCoins >= OustGame.FORCE_OUST_THRESHOLD) return false
        if (currentCoins < requiredCoins) return false
        return true
    }

    companion object {
        val values: List<OustAction> = values().toList()
    }
}

sealed class OustAction2 {
    data class SelectAction(val id: String, val action: OustAction)
    data class Contest(val id: String, val contesterId: String, val action: OustAction)
    data class NoContext(val id: String, val action: OustAction)
}

sealed class OustRequest {
    data class SelectAction(val actions: List<OustAction>): OustRequest()
}

sealed class OustResponse {
    fun validate(game: OustGame): Boolean = true

    object GoBack : OustResponse()
    data class SelectedAction(val action: OustAction) : OustResponse()
    data class SelectPlayer(val players: List<OustPlayer>) : OustResponse()
    data class Contest(val player: OustPlayer, val action: OustAction) : OustResponse()
    data class EndGame(val winner: OustPlayer) : OustResponse()
    data class TurnResponse(val response: OustTurnResponse): OustResponse()
}

sealed class OustTurnResponse {
    data class Coup(val player: OustPlayer): OustTurnResponse()
}