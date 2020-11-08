package ca.allanwang.discord.bot.time

import ca.allanwang.discord.bot.base.*
import ca.allanwang.discord.bot.core.BotFeature
import com.gitlab.kordlib.common.entity.Snowflake
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.behavior.channel.createEmbed
import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.entity.User
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.event.message.ReactionAddEvent
import com.google.common.flogger.FluentLogger
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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

        /**
         * A time regex should match values of the format
         * 8am, 9pm, 8:00, 8:00 am
         *
         * To avoid matching tags, time values should not be surrounded by alphanumeric characters.
         */
        val timeRegex = Regex("(?:^|[^a-zA-Z0-9])(1[0-2]|0?[1-9])(?::([0-5][0-9]))?\\s*([AaPp][Mm])?(?:$|[^a-zA-Z0-9])")
    }

    internal data class TimeEntry(val hour: Int, val minute: Int, val pm: Boolean?) {
        override fun toString(): String = buildString {
            append(hour)
            append(':')
            append(minute.toString().padStart(2, '0'))
            if (pm != null) {
                append(' ')
                append(if (pm) "PM" else "AM")
            }
        }

        val hour24: Int
            get() = when {
                hour == 12 -> if (pm == false) 0 else 12
                pm == true -> hour + 12
                else -> hour
            }

        fun toZonedDateTime(zoneId: ZoneId): ZonedDateTime =
            ZonedDateTime.of(LocalDate.now(zoneId), LocalTime.of(hour24, minute), zoneId)
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

    private fun String.findTimes(): List<TimeEntry> =
        timeRegex
            .findAll(this)
            .mapNotNull { it.toTimeEntry() }
            .distinct()
            .toList()

    private class TimeBotInfo(
        val authorId: Snowflake,
        val times: List<TimeEntry>,
        val origTimezone: TimeZone,
        val timezones: List<TimeZone>
    )

    private suspend fun Message.timeBotInfo(groupSnowflake: Snowflake): TimeBotInfo? {
        val authorId = author?.id ?: return null
        val times = content.findTimes()
        if (times.isEmpty()) return null
        logger.atInfo().log("Times matched %s - %s", times, id.value)
        val origTimezone = timeApi.getTime(groupSnowflake, authorId) ?: return null
        val timezones = timeApi.groupTimes(groupSnowflake)
        if (timezones.size <= 1) return null
        return TimeBotInfo(
            authorId = authorId,
            times = times,
            origTimezone = origTimezone,
            timezones = timezones
        )
    }

    private suspend fun MessageCreateEvent.handleEvent() {
        val info = message.timeBotInfo(groupSnowflake()) ?: return

        // To avoid spam, we limit auto messages to only occur during mentions
        if (message.mentionedRoleIds.isNotEmpty() || message.mentionedUserIds.isNotEmpty() || message.mentionsEveryone)
            message.createTimezoneMessage(info, user = message.author)
        else
            createTimezoneReaction()
    }

    private suspend fun Message.createTimezoneMessage(info: TimeBotInfo, user: User?) {
        channel.createEmbed {
            title = "Timezones"

            color = timeApi.embedColor

            val origZoneId = info.origTimezone.toZoneId()

            info.times.forEach { time ->

                val date = time.toZonedDateTime(origZoneId)

                field {
                    name = buildString {
                        appendQuote {
                            append(time.toString())
                        }
                    }
                    inline = true
                    value = buildString {
                        info.timezones.forEach { timezone ->
                            val zoneId = timezone.toZoneId()
                            appendOptional(timezone.rawOffset == info.origTimezone.rawOffset, this::appendUnderline) {
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

            if (user != null) {
                footer {
                    text = "Requested by ${user.tag}"
                }
            }
        }
    }

    private suspend fun MessageCreateEvent.createTimezoneReaction() {
        message.addReaction(timeApi.reactionEmoji)
        launch {
            withTimeoutOrNull(timeApi.reactionThresholdTime) {
                kord.events.filterIsInstance<ReactionAddEvent>()
                    .filter { it.messageId == message.id }
                    .first { it.handleEvent() }
            }
            message.deleteOwnReaction(timeApi.reactionEmoji)
            logger.atInfo().log("Remove listener for message %s", message.id.value)
        }
    }

    private suspend fun ReactionAddEvent.handleEvent(): Boolean {
        if (userId == kord.selfId) return false
        logger.atInfo().log("Receive pending event with emoji %s", emoji.name)
        if (emoji != timeApi.reactionEmoji) return false
        val user = getUserOrNull()
        if (user?.isBot == true) return false
        val message = getMessage()
        val info = message.timeBotInfo(message.groupSnowflake(guildId)) ?: return true
        logger.atInfo().log("Sending reaction response message")
        message.createTimezoneMessage(info, user = user)
        return true
    }
}
