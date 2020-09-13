package ca.allanwang.discord.bot.time

import ca.allanwang.discord.bot.base.*
import ca.allanwang.discord.bot.core.BotFeature
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.behavior.channel.createEmbed
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.google.common.flogger.FluentLogger
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
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

        fun toZonedDateTime(zoneId: ZoneId): ZonedDateTime =
            ZonedDateTime.of(LocalDate.now(zoneId), LocalTime.of(hour + (if (pm == true) 12 else 0), minute), zoneId)
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
        onMessage {
            handleEvent()
        }
    }

    private suspend fun MessageCreateEvent.handleEvent() {
        val authorId = message.author?.id ?: return
        val times = timeRegex
            .findAll(message.content)
            .mapNotNull { it.toTimeEntry() }
            .distinct()
            .toList()
        if (times.isEmpty()) return
        logger.atInfo().log("Times matched %s", times)
        val origZoneId =
            timeApi.getTime(groupSnowflake(), authorId)?.toZoneId() ?: return logger.atInfo().log("No user timezone")
        val timezones = timeApi.groupTimes(groupSnowflake())
        if (timezones.size <= 1) return logger.atInfo().log("No multiple group timezones")

        // To avoid spam, we limit auto messages to only occur during mentions
        if (message.mentionedRoleIds.isNotEmpty() || message.mentionedUserIds.isNotEmpty() || message.mentionsEveryone)
            createTimezoneMessage(origZoneId, times, timezones)
    }

    private suspend fun MessageCreateEvent.createTimezoneMessage(
        origZoneId: ZoneId,
        times: List<TimeEntry>,
        timezones: List<TimeZone>
    ) {
        message.channel.createEmbed {
            title = "Timezones"

            color = timeApi.embedColor

            times.forEach { time ->

                val date = time.toZonedDateTime(origZoneId)

                field {
                    name = buildString {
                        appendQuote {
                            append(time.toString())
                        }
                    }
                    inline = true
                    value = buildString {
                        timezones.forEach { timezone ->
                            val zoneId = timezone.toZoneId()
                            appendOptional(zoneId == origZoneId, this::appendUnderline) {
                                appendBold {
                                    append(timezone.displayName)
                                }
                            }
                            append(": ")
                            append(
                                date.withZoneSameInstant(zoneId)
                                    .format(
                                        if (time.pm == null) timeApi.dateTimeFormatterNoAmPm
                                        else timeApi.dateTimeFormatter
                                    )
                            )
                            appendLine()
                        }
                    }.trim()
                }
            }
        }
    }
}
