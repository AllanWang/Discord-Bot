package ca.allanwang.discord.bot.cinco.game

import ca.allanwang.discord.bot.base.BotPrefixSupplier
import ca.allanwang.discord.bot.base.appendBold
import ca.allanwang.discord.bot.cinco.CincoScope
import ca.allanwang.discord.bot.cinco.game.core.CincoMessageBehavior
import ca.allanwang.discord.bot.cinco.game.core.CincoPointTracker
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.google.common.flogger.FluentLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
    private val prefixSupplier: BotPrefixSupplier,
) : CincoGame {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    override suspend fun start() {
        logger.atInfo().log("Start")
        val roundTotal = 15
        val cancellable = kord.launch {
            kord.events.filterIsInstance<MessageCreateEvent>()
                .filter { it.message.channelId == cincoMessageBehavior.id }
                .filter { it.message }
        }
        (1..roundTotal).forEach {
            launchRound(it, roundTotal)
            delay(2000)
        }
        cincoMessageBehavior.showStandings {
            title = "Game Ended"
        }
    }

    private suspend fun launchRound(round: Int, roundTotal: Int) {
        val rnd: Random = ThreadLocalRandom.current().asKotlinRandom()
        val word = wordBank.getWord(rnd)
        val scrambledWord =
            generateSequence { word.toList().shuffled(rnd).joinToString("") }
                .take(10)
                .first { it != word }
        val entry = withTimeoutOrNull(15_000) {
            cincoMessageBehavior.createCincoEmbed {
                title = scrambledWord
                description = variant.description
                footer {
                    text = "$round/$roundTotal"
                }
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
            footer {
                text = "$round/$roundTotal"
            }
        }
    }
}