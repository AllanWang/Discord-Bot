package ca.allanwang.discord.bot.cinco.game

import ca.allanwang.discord.bot.cinco.CincoScope
import ca.allanwang.discord.bot.cinco.game.core.CincoMessageBehavior
import ca.allanwang.discord.bot.cinco.game.features.CincoGameFeature
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.google.common.flogger.FluentLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException
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
        var roundIndex: Int = -1
        var roundJob: Job? = null
        val cancellable = kord.launch {
            val skipCommand = "${cincoContext.botPrefix}skip"
            kord.events.filterIsInstance<MessageCreateEvent>()
                .filter { it.message.channelId == cincoMessageBehavior.id }
                .filter { it.message.author?.isBot != true }
                .onEach { logger.atInfo().log("Cancellable logged message") }
                .filter { it.message.content == skipCommand }
                .collect {
                    roundJob?.let {
                        it.cancel(CancellationException("Requested skip"))
                        feature.skip(roundIndex)
                    }
                    roundJob = null
                }
        }
        (1..cincoContext.gameRounds).forEach {
            roundIndex = it
            roundJob = kord.launch {
                feature.startRound(roundIndex)
                delay(2000)
            }
            roundJob?.join()
        }
        roundIndex = -1
        roundJob = null

        feature.endGame()
        cancellable.cancel()
    }

}

