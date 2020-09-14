package ca.allanwang.discord.bot.time

import ca.allanwang.discord.bot.base.*
import ca.allanwang.discord.bot.core.BotFeature
import com.gitlab.kordlib.common.entity.Snowflake
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.behavior.channel.createEmbed
import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.event.message.ReactionAddEvent
import com.gitlab.kordlib.core.on
import com.google.common.flogger.FluentLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
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
                append(if (pm) "PM" else "AM")
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

    private val pendingReactionCache: ConcurrentHashMap<Snowflake, Long> = ConcurrentHashMap()

    override suspend fun Kord.attach() {
        onMessage {
            handleEvent()
        }
        on<ReactionAddEvent> {
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
        val origZoneId: ZoneId,
        val timezones: List<TimeZone>
    )

    private suspend fun Message.timeBotInfo(groupSnowflake: Snowflake): TimeBotInfo? {
        val authorId = author?.id ?: return null
        val times = content.findTimes()
        if (times.isEmpty()) return null
        logger.atInfo().log("Times matched %s", times)
        val origZoneId = timeApi.getTime(groupSnowflake, authorId)?.toZoneId() ?: return null
        val timezones = timeApi.groupTimes(groupSnowflake)
        if (timezones.size <= 1) return null
        return TimeBotInfo(
            authorId = authorId,
            times = times,
            origZoneId = origZoneId,
            timezones = timezones
        )
    }

    private suspend fun MessageCreateEvent.handleEvent() {
        val info = message.timeBotInfo(groupSnowflake()) ?: return

        // To avoid spam, we limit auto messages to only occur during mentions
        if (message.mentionedRoleIds.isNotEmpty() || message.mentionedUserIds.isNotEmpty() || message.mentionsEveryone)
            message.createTimezoneMessage(info)
        else
            createTimezoneReaction()
    }

    private suspend fun Message.createTimezoneMessage(info: TimeBotInfo) {
        channel.createEmbed {
            title = "Timezones"

            color = timeApi.embedColor

            info.times.forEach { time ->

                val date = time.toZonedDateTime(info.origZoneId)

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
                            appendOptional(zoneId == info.origZoneId, this::appendUnderline) {
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

    private suspend fun MessageCreateEvent.createTimezoneReaction() {
        message.addReaction(timeApi.reactionEmoji)
        pendingReactionCache[message.id] = System.currentTimeMillis()
        launch {
            delay(timeApi.reactionThresholdTime)
            pendingReactionCache.remove(message.id)
            message.deleteOwnReaction(timeApi.reactionEmoji)
        }
    }

    private suspend fun ReactionAddEvent.handleEvent() {
        if (!pendingReactionCache.containsKey(message.id)) return
        logger.atInfo().log("Receive pending event with emoji %s", emoji.name)
        if (emoji.name != timeApi.reactionEmoji.name) return
        if (getUserOrNull()?.isBot == true) return
        logger.atInfo().log("Received reaction response")
        val message = getMessage()
        val info = message.timeBotInfo(message.groupSnowflake(guildId)) ?: return
        logger.atInfo().log("Sending reaction response message")
        pendingReactionCache.remove(message.id)
        message.deleteReaction(timeApi.reactionEmoji)
        message.createTimezoneMessage(info)
    }
}
