package ca.allanwang.discord.bot.cinco.game

import ca.allanwang.discord.bot.cinco.CincoScope
import ca.allanwang.discord.bot.cinco.game.core.CincoMessageBehavior
import ca.allanwang.discord.bot.cinco.game.features.CincoGameFeature
import com.gitlab.kordlib.core.Kord
import com.google.common.flogger.FluentLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.selects.select
import java.time.Instant
import javax.inject.Inject

@CincoScope
class CincoGame @Inject constructor(
    private val kord: Kord,
    private val feature: CincoGameFeature,
    private val cincoMessageBehavior: CincoMessageBehavior,
    private val cincoContext: CincoContext
) {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    private val skipCommand = "${cincoContext.botPrefix}skip"
    private val endCommand = "${cincoContext.botPrefix}end"

    private enum class ShortCircuit {
        Skip, End
    }

    suspend fun start() {
        val gameJob = Job()
        kord.launch(gameJob) {
            (1..cincoContext.gameRounds).forEach { i ->
                logger.atFine().log("Start round %d", i)
                val shortCircuit = async { shortCircuit() }
                val cincoRound = async { feature.startRound(i) }
                select<Unit> {
                    shortCircuit.onAwait {
                        when (it) {
                            ShortCircuit.Skip -> {
                                cincoRound.cancel("Requested skip")
                                feature.skip(i)
                            }
                            ShortCircuit.End -> {
                                gameJob.cancel("Requested end")
                                feature.endGame()
                            }
                        }
                    }
                    cincoRound.onAwait {
                        logger.atInfo().log("cinco round")
                        shortCircuit.cancel("Round completed")
                    }
                }
                delay(2000)
            }
            feature.endGame()
        }
    }

    private suspend fun shortCircuit(): ShortCircuit {
        logger.atFine().log("Subscribe to short circuit %s", Instant.now())
        return cincoMessageBehavior.playerMessageFlow()
            .mapNotNull {
                val message = it.message.content.trim()
                when {
                    message.equals(skipCommand, ignoreCase = true) -> ShortCircuit.Skip
                    message.equals(endCommand, ignoreCase = true) -> ShortCircuit.End
                    else -> null
                }
            }.first()
    }

}

