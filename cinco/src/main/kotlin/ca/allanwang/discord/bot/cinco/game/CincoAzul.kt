package ca.allanwang.discord.bot.cinco.game

import ca.allanwang.discord.bot.base.appendBold
import ca.allanwang.discord.bot.cinco.CincoScope
import ca.allanwang.discord.bot.cinco.game.core.CincoMessageBehavior
import ca.allanwang.discord.bot.cinco.game.core.CincoPointTracker
import com.google.common.flogger.FluentLogger
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ThreadLocalRandom
import javax.inject.Inject
import kotlin.random.Random
import kotlin.random.asKotlinRandom

@CincoScope
class CincoAzul @Inject constructor(
    private val variant: CincoVariant,
    private val wordBank: WordBank,
    private val cincoMessageBehavior: CincoMessageBehavior,
    private val pointTracker: CincoPointTracker
) : CincoGame {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    override suspend fun start() {
        logger.atInfo().log("Start")
        launchRound()
    }

    private suspend fun launchRound() {
        val rnd: Random = ThreadLocalRandom.current().asKotlinRandom()
        val word = wordBank.getWord(rnd)
        val scrambledWord =
            generateSequence { word.toList().shuffled(rnd).joinToString("") }
                .take(10)
                .first { it != word }
        val entry = withTimeoutOrNull(3000) {
            cincoMessageBehavior.createCincoEmbed {
                title = scrambledWord
                description = variant.description
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
        }
    }
}