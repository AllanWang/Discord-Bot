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
class CoinBot @Inject constructor() : CommandHandlerBot {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()

        private inline val rnd: Random get() = ThreadLocalRandom.current()

        private val embedColor = Color(0xFFEEB501.toInt())
    }

    override val handler =
        commandBuilder("flip", embedColor, CommandHandler.Type.Prefix, description = "RNG coin") {
            action(
                withMessage = true,
                help = {
                    buildString {
                        append("Flip heads or tails. ")
                        append("Optionally provide one of ")
                        val options = listOf("h", "heads", "t", "tails")
                        options.forEachIndexed { index, s ->
                            if (index != 0) {
                                append(",")
                            }
                            if (index == options.lastIndex) {
                                append(" or")
                            }
                            append(" ")
                            appendCodeBlock { append(s) }
                        }
                        append(" to mark a selection.")
                    }
                }
            ) {
                when (message.trim().toLowerCase()) {
                    "h", "heads" -> flipCoin(true)
                    "t", "tails" -> flipCoin(false)
                    else -> flipCoin(null)
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
}
