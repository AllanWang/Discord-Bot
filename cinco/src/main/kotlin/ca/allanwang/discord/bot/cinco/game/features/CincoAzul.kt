package ca.allanwang.discord.bot.cinco.game.features

import ca.allanwang.discord.bot.base.appendBold
import ca.allanwang.discord.bot.cinco.CincoScope
import ca.allanwang.discord.bot.cinco.game.CincoContext
import ca.allanwang.discord.bot.cinco.game.WordBank
import ca.allanwang.discord.bot.cinco.game.core.CincoEntry
import ca.allanwang.discord.bot.cinco.game.core.CincoMessageBehavior
import ca.allanwang.discord.bot.cinco.game.core.CincoPointTracker
import com.google.common.flogger.FluentLogger
import kotlinx.coroutines.flow.firstOrNull
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
    private val pointTracker: CincoPointTracker,
    override val cincoContext: CincoContext
) : CincoGameFeature {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    private val roundWords: MutableList<String> = mutableListOf()

    override suspend fun startRound(round: Int) {
        val rnd: Random = ThreadLocalRandom.current().asKotlinRandom()
        val word = wordBank.getWord(rnd)
        val charList = word.toList().sorted()
        val scrambledWord =
            generateSequence { charList.shuffled(rnd).joinToString("") }
                .take(10)
                .first { it != word }
        roundWords.add(word)
        val entry = withTimeoutOrNull(cincoContext.roundTimeout) {
            cincoMessageBehavior.createCincoEmbed {
                title = scrambledWord
                description = variant.description
                roundProgressFooter(round)
            }.firstOrNull {
                val guess = it.word
                /*
                 * While each round has a specific word in mind,
                 * I think it's more fair that any valid word should count.
                 */
                guess == word || guess.toList().sorted() == charList && wordBank.isWord(guess)
            }
        }

        roundResult(round = round, word = entry?.word ?: word, entry = entry, skipped = false)
    }

    override suspend fun skip(round: Int) {
        val word = roundWords.getOrNull(round - 1)
        if (word == null) {
            logger.atWarning().log("Invalid skip for round %d", round)
            return
        }
        roundResult(round = round, word = word, entry = null, skipped = true)
    }

    private suspend fun roundResult(round: Int, word: String, entry: CincoEntry?, skipped: Boolean) {
        cincoMessageBehavior.createEmbed {
            title = buildString {
                append("Round over")
                if (skipped) append(" (Skipped)")
            }
            description = buildString {
                if (!skipped) {
                    if (entry == null) {
                        append("No one unscrambled the word.")
                    } else {
                        appendLine("${entry.player.mention} guessed the word")
                        val totalPoints = pointTracker.addPoints(entry.player, 1)
                        append("+1 score, total $totalPoints")
                    }
                    appendLine()
                }
                append("Answer: ")
                appendBold {
                    append(word)
                }
            }
            roundProgressFooter(round)
        }
    }

    override suspend fun endGame() {
        cincoMessageBehavior.showStandings {
            title = "Game Ended"
        }
    }
}