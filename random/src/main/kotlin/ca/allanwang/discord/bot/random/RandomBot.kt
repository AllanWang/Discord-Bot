package ca.allanwang.discord.bot.random

import ca.allanwang.discord.bot.base.CommandHandler
import ca.allanwang.discord.bot.base.CommandHandlerBot
import ca.allanwang.discord.bot.base.CommandHandlerEvent
import ca.allanwang.discord.bot.base.commandBuilder
import com.gitlab.kordlib.core.behavior.channel.createEmbed
import com.google.common.flogger.FluentLogger
import java.awt.Color
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RandomBot @Inject constructor(

) : CommandHandlerBot {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()

        private inline val rnd: Random get() = ThreadLocalRandom.current()

        private val rangeRegex = Regex("^\\s*(\\d+)[\\s-]+(\\d+)\\s*$")

        private val embedColor = Color.decode("#EEB501")
    }

    override val handler = commandBuilder(CommandHandler.Type.Prefix) {
        arg("flip") {
            action(withMessage = true) {
                when (message.trim().toLowerCase()) {
                    "h", "heads" -> flipCoin(true)
                    "t", "tails" -> flipCoin(false)
                    else -> flipCoin(null)
                }
            }
        }
        arg("roll") {
            action(withMessage = true) {
                val range = rangeRegex.find(message)?.let { it.groupValues[1].toInt() to it.groupValues[2].toInt() }
                    ?: 1 to 6
                roll(range.first, range.second)
            }
        }
    }

    private suspend fun CommandHandlerEvent.flipCoin(expectHeads: Boolean?) {
        val heads = rnd.nextBoolean()
        channel.createEmbed {
            title = if (heads) "Heads" else "Tails"
            color = embedColor
            if (expectHeads != null) {
                description = "You ${if (expectHeads == heads) "win!" else "lose!"}"
            }
            userFooter()
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
