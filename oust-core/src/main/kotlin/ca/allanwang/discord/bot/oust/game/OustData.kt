package ca.allanwang.discord.bot.oust.game

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
        with(move) { apply() }
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size
    }

    companion object {

        private inline val rnd: Random get() = ThreadLocalRandom.current()

        private const val STARTING_COINS: Int = 2

        const val OUST_COST: Int = 7

        const val FORCE_OUST_THRESHOLD: Int = 10

        const val ASSASSINATION_COST = 3

        const val STEAL_AMOUNT = 2

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

    data class Info(val id: String, val name: String)
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