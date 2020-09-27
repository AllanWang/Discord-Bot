package ca.allanwang.discord.bot.cinco.game

import com.google.common.flogger.FluentLogger
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class WordBank @Inject constructor() {

    companion object {
        private const val WORD_FILE = "cinco_word_bank.txt"
        private val logger = FluentLogger.forEnclosingClass()
    }

    private val words: Set<String> =
        this::class.java.classLoader.getResourceAsStream(WORD_FILE)?.bufferedReader()?.use { reader ->
            reader.lineSequence().filter { !it.startsWith('#') }.onEach {
                if (it.length != 5) logger.atWarning().log("Non 5 letter word '%s'", it)
            }.toSet()
        } ?: run {
            logger.atWarning().log("Could not load %s", WORD_FILE)
            emptySet()
        }

    fun isWord(word: String): Boolean = word.toLowerCase(Locale.US) in words

    fun getWord(rnd: Random): String = words.random(rnd)
}