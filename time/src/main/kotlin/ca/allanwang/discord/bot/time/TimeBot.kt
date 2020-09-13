package ca.allanwang.discord.bot.time

import ca.allanwang.discord.bot.core.BotFeature
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.on
import com.google.common.flogger.FluentLogger
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeBot @Inject constructor(
    private val timeApi: TimeApi
) : BotFeature {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()

        val timeRegex = Regex("(1[0-2]|0?[1-9])(?::([0-5][0-9]))?\\s*([AaPp][Mm])?")
    }

    private data class TimeEntry(val hour: Int, val minute: Int, val pm: Boolean?) {
        override fun toString(): String = buildString {
            append(hour)
            append(':')
            append(minute.toString().padStart(2, '0'))
            if (pm != null) {
                append(' ')
                append(if (pm) "pm" else "am")
            }
        }
    }

    private fun MatchResult.toTimeEntry(): TimeEntry? {
        // Disallow general numbers as timestamps
        if (groupValues[2].isEmpty() && groupValues[3].isEmpty()) return null
        val hour = groupValues[1].toInt()
        val minute = groupValues[2].toIntOrNull() ?: 0
        val pm = when (groupValues[3].toLowerCase(Locale.US)) {
            "am" -> false
            "pm" -> true
            else -> null
        }
        return TimeEntry(hour, minute, pm)
    }

    override suspend fun Kord.attach() {
        on<MessageCreateEvent> {
            if (message.author?.isBot == true) return@on
            val times = timeRegex.findAll(message.content).mapNotNull { it.toTimeEntry() }.toList()
            if (times.isEmpty()) return@on
            logger.atInfo().log("Times matched %s", times)
        }
    }
}
