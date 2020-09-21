package ca.allanwang.discord.bot.oust.game

import com.gitlab.kordlib.common.entity.Snowflake
import java.util.*
import java.util.concurrent.ThreadLocalRandom

data class OustGame(
    var currentPlayerIndex: Int,
    val players: List<OustPlayer>,
    var deck: List<OustCard>,
    var discardDeck: List<OustCard>
) {

    val currentPlayer: OustPlayer get() = players[currentPlayerIndex]

    fun move(move: OustMove) {
        when (move) {
            is OustMove.Oust -> {
                currentPlayer.spendCoins(OUST_COST)
                discardCard(move.player, move.cardIndex)
            }
            is OustMove.Assassinate -> {
                currentPlayer.spendCoins(ASSASSINATION_COST)
                discardCard(move.player, move.cardIndex)
            }
        }
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size
    }

    private fun discardCard(id: Snowflake, cardIndex: Int) {
        val player = players.firstOrNull { it.info.id == id }
            ?: throw IllegalArgumentException("Could not find player with matching id")
        player.cards.removeAt(cardIndex)
    }

    companion object {

        private inline val rnd: Random get() = ThreadLocalRandom.current()

        private const val STARTING_COINS: Int = 2

        const val OUST_COST: Int = 7

        const val ASSASSINATION_COST = 3

        fun create(players: List<OustPlayer.Info>): OustGame {
            if (players.size < 3) throw IllegalArgumentException("Oust requires at least 3 players")
            val maxPlayers = (OustCard.deck.size - 3) / 2
            if (players.size > maxPlayers) throw IllegalArgumentException("Oust can have at most $maxPlayers players")
            val shuffledPlayers = players.shuffled(rnd)
            val fullDeck = LinkedList(OustCard.deck.shuffled(rnd))
            val oustPlayers: List<OustPlayer> = shuffledPlayers.map { info ->
                val cards = listOf(fullDeck.pop(), fullDeck.pop()).map { OustPlayer.Card(it, visible = false) }
                OustPlayer(info = info, cards = cards.toMutableList(), coins = STARTING_COINS)
            }
            return OustGame(
                currentPlayerIndex = 0,
                players = oustPlayers,
                deck = fullDeck,
                discardDeck = emptyList()
            )
        }
    }
}

data class OustPlayer(val info: Info, val cards: MutableList<Card>, var coins: Int) {

    fun spendCoins(coins: Int) {
        check(this.coins >= coins) { "Player has insufficient balance" }
        this.coins -= coins
    }

    data class Card(val value: OustCard, var visible: Boolean)

    data class Info(val id: Snowflake, val name: String)
}

enum class OustCard {
    Assassin,
    BodyGuard,
    Thief,
    Banker,
    Equalizer;

    companion object {
        /**
         * Default deck has three of each card
         */
        val deck: List<OustCard> = values().toList() + values().toList() + values().toList()
    }
}

sealed class OustMove {

    data class Oust(val player: Snowflake, val cardIndex: Int) : OustMove()
    data class Assassinate(val player: Snowflake, val cardIndex: Int) : OustMove()
    data class Steal(val player: Snowflake) : OustMove()

}