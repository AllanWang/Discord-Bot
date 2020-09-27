package ca.allanwang.discord.bot.cinco.game

import ca.allanwang.discord.bot.cinco.CincoScope
import ca.allanwang.discord.bot.cinco.game.core.CincoMessageBehavior
import ca.allanwang.discord.bot.cinco.game.features.CincoGameFeature
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.google.common.flogger.FluentLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.onEach
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

    suspend fun start() {
        logger.atInfo().log("Start")
        val gameJob = SupervisorJob()
        kord.launch(gameJob) {
            var roundIndex: Int = -1
            var roundJob: Job? = null
            launch {
                val skipCommand = "${cincoContext.botPrefix}skip"
                val endCommand = "${cincoContext.botPrefix}end"
                kord.events.filterIsInstance<MessageCreateEvent>()
                    .filter { it.message.channelId == cincoMessageBehavior.id }
                    .filter { it.message.author?.isBot != true }
                    .onEach { logger.atInfo().log("Cancellable logged message") }
                    .filter { it.message.content == skipCommand }
                    .collect {
                        val message = it.message.content.trim()
                        if (message.equals(skipCommand, ignoreCase = true)) {
                            roundJob?.let { job ->
                                job.cancel(CancellationException("Requested skip"))
                                feature.skip(roundIndex)
                            }
                            roundJob = null
                        } else if (message.equals(endCommand, ignoreCase = true)) {
                            throw CancellationException("Request game end")
                        }
                    }
            }
            (1..cincoContext.gameRounds).forEach {
                roundIndex = it
                roundJob = launch {
                    feature.startRound(roundIndex)
                    delay(2000)
                }
                try {
                    roundJob?.join()
                } catch (e: CancellationException) {
                    // ignore
                    logger.atInfo().log("round cancel %s", e.message)
                }
            }
            roundIndex = -1
            roundJob = null
            throw CancellationException("Game ended normally")
        }

        try {
            gameJob.join()
        } catch (e: CancellationException) {
            // ignore
            logger.atInfo().log("game cancel %s", e.message)
        }
        feature.endGame()
    }

}

