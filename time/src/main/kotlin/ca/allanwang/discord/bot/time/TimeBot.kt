package ca.allanwang.discord.bot.time

import ca.allanwang.discord.bot.base.*
import ca.allanwang.discord.bot.core.BotFeature
import com.google.common.flogger.FluentLogger
import dev.kord.common.entity.MessageType
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.ReactionAddEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeBot @Inject constructor(
    private val timeApi: TimeApi
) : BotFeature {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    override suspend fun Kord.attach() {
        onMessage {
            handleEvent()
        }
    }

    private class TimeBotInfo(
        val authorId: Snowflake,
        val times: List<TimeApi.TimeEntry>,
        val origTimezone: TimeZone,
        val timezones: List<TimeZone>
    )

    private suspend fun Message.timeBotInfo(groupSnowflake: Snowflake): TimeBotInfo? {
        val authorId = author?.id ?: return null
        val times = timeApi.findTimes(content)
        if (times.isEmpty()) return null
        logger.atFine().log("Times matched %s - %d", times, id.value)
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
        if (message.hasMention)
            message.createTimezoneMessage(info, user = message.author)
        else
            createTimezoneReaction()
    }

    private val Message.hasMention: Boolean
        get() {
            // Do not accept user mentions if from replies
            if (mentionedUserIds.size >= 2) return type != MessageType.Reply
            return mentionedRoleIds.isNotEmpty() || mentionsEveryone
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
                message.reactionAddEvents()
                    .first { it.handleEvent() }
            }
            message.deleteOwnReaction(timeApi.reactionEmoji)
            logger.atFine().log("Remove listener for message %d", message.id.value)
        }
    }

    private suspend fun ReactionAddEvent.handleEvent(): Boolean {
        logger.atFine().log("Receive pending event with emoji %s", emoji.name)
        if (emoji != timeApi.reactionEmoji) return false
        val user = getUserOrNull()
        if (user?.isBot == true) return false
        val message = getMessage()
        val info = message.timeBotInfo(message.groupSnowflake(guildId)) ?: return true
        logger.atFine().log("Sending reaction response message")
        message.createTimezoneMessage(info, user = user)
        return true
    }
}
