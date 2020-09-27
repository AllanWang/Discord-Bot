package ca.allanwang.discord.bot.cinco.game

import com.google.common.flogger.FluentLogger
import java.util.*
import javax.inject.Singleton

@Singleton
class WordBank {

    companion object {
        private const val WORD_FILE = "cinco_word_bank.txt"
        private val logger = FluentLogger.forEnclosingClass()
    }

    private val words: Set<String> =
        this::class.java.classLoader.getResourceAsStream(WORD_FILE)?.bufferedReader()?.use { reader ->
            reader.lineSequence().filter { !it.startsWith('#') }.map { it.trim().toLowerCase() }.toSet()
        } ?: run {
            logger.atWarning().log("Could not load %s", WORD_FILE)
            emptySet()
        }

    fun isWord(word: String): Boolean = word.toLowerCase(Locale.US) in words

}