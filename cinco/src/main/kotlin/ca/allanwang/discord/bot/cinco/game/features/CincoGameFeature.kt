package ca.allanwang.discord.bot.cinco.game.features

import ca.allanwang.discord.bot.cinco.game.CincoContext
import dev.kord.rest.builder.message.EmbedBuilder

interface CincoGameFeature {
    val cincoContext: CincoContext
    suspend fun startRound(round: Int)
    suspend fun skip(round: Int)
    suspend fun endGame()
    fun EmbedBuilder.roundProgressFooter(round: Int) {
        footer {
            text = "$round/${cincoContext.gameRounds}"
        }
    }
}

