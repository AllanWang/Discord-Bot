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

enum class OustAction(val requiredCoins: Int = 0, val blockable: Boolean = true) {
    Oust(requiredCoins = OustGame.OUST_COST, blockable = false),
    Assassinate(requiredCoins = OustGame.ASSASSINATION_COST),
    Steal,
    PayDay(blockable = false),
    BigPayDay,
//    Equalize,
    Shuffle;

    fun isPossible(player: OustPlayer): Boolean {
        val currentCoins = player.coins
        if (this@OustAction == Oust) return currentCoins >= OustGame.OUST_COST
        if (currentCoins >= OustGame.FORCE_OUST_THRESHOLD) return false
        if (currentCoins < requiredCoins) return false
        return true
    }

    companion object {
        val values: List<OustAction> = values().toList()
    }
}

enum class KillType {
    Assassin, Oust
}

sealed class OustRequest {
    data class SelectAction(val actions: List<OustAction>) : OustRequest()
    data class SelectPlayerKill(val players: List<OustPlayer>, val type: KillType) : OustRequest()
    data class SelectCardsShuffle(val deckCards: List<OustCard>) : OustRequest()
    data class SelectPlayerSteal(val players: List<OustPlayer>) : OustRequest()
}

sealed class OustResponse {
    object GoBack : OustResponse()
    data class SelectedAction(val action: OustAction) : OustResponse()
    data class TurnResponse(val response: OustTurnResponse) : OustResponse()
}

sealed class OustTurnResponse {

    data class KillPlayer(val player: OustPlayer, val type: KillType) : OustTurnResponse()
    object PayDay : OustTurnResponse()
    object BigPayDay : OustTurnResponse()
    data class Steal(val player: OustPlayer) : OustTurnResponse()
}

sealed class OustTurnRebuttal {
    object Allow : OustTurnRebuttal()
    data class Decline(val player: OustPlayer, val card: OustCard) : OustTurnRebuttal()
}