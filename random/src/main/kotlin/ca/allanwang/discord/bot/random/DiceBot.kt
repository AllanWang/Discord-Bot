package ca.allanwang.discord.bot.random

import ca.allanwang.discord.bot.base.*
import com.google.common.flogger.FluentLogger
import dev.kord.common.Color
import dev.kord.core.behavior.channel.createEmbed
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiceBot @Inject constructor() : CommandHandlerBot {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()

        private inline val rnd: Random get() = ThreadLocalRandom.current()

        private val rangeRegex = Regex("^\\s*(\\d+)(?:[\\s-]+(\\d+))?\\s*$")

        private val embedColor = Color(0xFFEEB501.toInt())

        /**
         * Extract start and end range from input.
         * If only one number is provided, we assume the roll is 1 to the number.
         */
        fun rollRange(input: String): Pair<Int, Int>? {
            val match = rangeRegex.find(input) ?: return null
            val first = match.groupValues[1].toInt()
            val second = match.groupValues[2].toIntOrNull()
            return if (second == null) 1 to first
            else first to second
        }
    }

    override val handler =
        commandBuilder("roll", embedColor, CommandHandler.Type.Prefix, description = "RNG die") {
            action(
                withMessage = true,
                help = {
                    buildString {
                        append("Roll a die. ")
                        append("Optionally provide two numbers to change the range (both inclusive). ")
                        append("Eg ")
                        appendCodeBlock {
                            append(prefix)
                            append("roll 1 6")
                        }
                        append(". ")
                        append("If only one number is provided, the range will start with 1 and end with that number (")
                        appendCodeBlock {
                            append(prefix)
                            append("roll 8 = ")
                            append(prefix)
                            append("roll 1 8")
                        }
                        append(").")
                    }
                }
            ) {
                val range = rollRange(message) ?: 1 to 6
                roll(range.first, range.second)
            }
        }

    private suspend fun CommandHandlerEvent.roll(min: Int, max: Int) {
        if (min >= max) {
            channel.createMessage("Invalid roll range ($min, $max)")
            return
        }
        val result = rnd.nextInt(max - min + 1) + min
        channel.createEmbed {
            title = result.toString()
            description = "Rolled from $min to $max"
            color = embedColor
            userFooter()
        }
    }
}
