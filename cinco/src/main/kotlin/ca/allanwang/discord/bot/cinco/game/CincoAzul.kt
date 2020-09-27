package ca.allanwang.discord.bot.cinco.game

import ca.allanwang.discord.bot.base.appendBold
import ca.allanwang.discord.bot.cinco.CincoScope
import ca.allanwang.discord.bot.cinco.game.core.CincoMessageBehavior
import ca.allanwang.discord.bot.cinco.game.core.CincoPointTracker
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.rest.builder.message.EmbedBuilder
import com.google.common.flogger.FluentLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.CancellationException
import java.util.concurrent.ThreadLocalRandom
import javax.inject.Inject
import kotlin.random.Random
import kotlin.random.asKotlinRandom

@CincoScope
class CincoAzul @Inject constructor(
    private val kord: Kord,
    private val variant: CincoVariant,
    private val wordBank: WordBank,
    private val cincoMessageBehavior: CincoMessageBehavior,
    private val pointTracker: CincoPointTracker,
    private val cincoContext: CincoContext
) : CincoGame {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    private fun EmbedBuilder.roundProgressFooter(round: Int) {
        footer {
            text = "$round/${cincoContext.gameRounds}"
        }
    }

    override suspend fun start() {
        logger.atInfo().log("Start")
        var roundIndex: Int = -1
        var roundJob: Job? = null
        val cancellable = kord.launch {
            val skipCommand = "${cincoContext.botPrefix}skip"
            kord.events.filterIsInstance<MessageCreateEvent>()
                .filter { it.message.channelId == cincoMessageBehavior.id }
                .onEach { logger.atInfo().log("Cancellable logged message") }
                .filter { it.message.content == skipCommand }
                .takeWhile { roundJob != null }
                .collect {
                    roundJob?.let {
                        it.cancel(CancellationException("Requested skip"))
                        cincoMessageBehavior.createEmbed {
                            title = "Round skipped"
                            roundProgressFooter(roundIndex)
                        }
                    }
                }
        }
        (1..cincoContext.gameRounds).forEach {
            roundIndex = it
            roundJob = kord.launch {
                launchRound(it)
                delay(2000)
            }
            roundJob?.join()
        }
        roundIndex = -1
        roundJob = null
        cincoMessageBehavior.showStandings {
            title = "Game Ended"
        }
        cancellable.cancel()
    }

    private suspend fun launchRound(round: Int) {
        val rnd: Random = ThreadLocalRandom.current().asKotlinRandom()
        val word = wordBank.getWord(rnd)
        val scrambledWord =
            generateSequence { word.toList().shuffled(rnd).joinToString("") }
                .take(10)
                .first { it != word }
        val entry = withTimeoutOrNull(cincoContext.roundTimeout) {
            cincoMessageBehavior.createCincoEmbed {
                title = scrambledWord
                description = variant.description
                roundProgressFooter(round)
            }.onEach { logger.atInfo().log("Entry %s", it) }
                .firstOrNull { it.word == word }
        }

        cincoMessageBehavior.createEmbed {
            title = "Round over"
            description = buildString {
                if (entry == null) {
                    append("No one unscrambled the word.")
                } else {
                    appendLine("${entry.player.username} guessed the word")
                    val totalPoints = pointTracker.addPoints(entry.player, 1)
                    append("+1 score, total $totalPoints")
                }
                appendLine()
                appendLine()
                append("Answer: ")
                appendBold {
                    append(word)
                }
            }
            roundProgressFooter(round)
        }
    }
}