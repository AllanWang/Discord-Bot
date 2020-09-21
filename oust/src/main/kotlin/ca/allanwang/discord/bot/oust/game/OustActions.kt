package ca.allanwang.discord.bot.oust.game

import com.gitlab.kordlib.common.entity.Snowflake

enum class OustAction(val requiredCoins: Int = 0, val blockable: Boolean = true) {
    Oust(requiredCoins = OustGame.OUST_COST, blockable = false),
    Assassinate(requiredCoins = OustGame.ASSASSINATION_COST),
    Steal,
    PayDay(blockable = false),
    BigPayDay,
    Equalize,
    Shuffle;

    companion object {
        val values: List<OustAction> = values().toList()
    }
}

sealed class OustAction2 {
    data class SelectAction(val id: Snowflake, val action: OustAction)
    data class Contest(val id: Snowflake, val contesterId: Snowflake, val action: OustAction)
    data class NoContext(val id: Snowflake, val action: OustAction)
}

sealed class OustReaction {
    fun validate(game: OustGame): Boolean = true

    data class SelectAction(val player: OustPlayer, val actions: List<OustAction>) : OustReaction()
    data class SelectPlayer(val player: OustPlayer, val players: List<OustPlayer>) : OustReaction()
    data class Contest(val player: OustPlayer, val action: OustAction) : OustReaction()
    data class EndGame(val winner: OustPlayer) : OustReaction()
}

fun OustGame.validActions(): List<OustAction> {
    val currentCoins = currentPlayer.coins
    if (currentCoins >= 10) return listOf(OustAction.Oust)
    return OustAction.values.filter { currentCoins >= it.requiredCoins }
}
