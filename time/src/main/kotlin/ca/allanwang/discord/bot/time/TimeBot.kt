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
    private val timeApi: TimeApi,
    private val prefixSupplier: BotPrefixSupplier,
    private val timeConfigBot: TimeConfigBot,
) : BotFeature {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    override suspend fun Kord.attach() {
        onMessage {
            handleEvent()
        }
    }

    sealed class TimeResult {
        data class Info(
            val authorId: Snowflake,
            val times: List<TimeApi.TimeEntry>,
            val origTimezone: TimeZone,
            val timezones: List<TimeZone>
        ) : TimeResult()

        object MissingTimezone : TimeResult()
    }

    private suspend fun Message.timeBotInfo(groupSnowflake: Snowflake): TimeResult? {
        val authorId = author?.id ?: return null
        val times = timeApi.findTimes(content)
        if (times.isEmpty()) return null
        logger.atFine().log("Times matched %s - %d", times, id.value)
        val origTimezone = timeApi.getTime(groupSnowflake, authorId) ?: return TimeResult.MissingTimezone
        val timezones = timeApi.groupTimes(groupSnowflake)
        if (timezones.size <= 1) return null
        return TimeResult.Info(
            authorId = authorId,
            times = times,
            origTimezone = origTimezone,
            timezones = timezones
        )
    }

    private suspend fun MessageCreateEvent.handleEvent() {
        val info = message.timeBotInfo(groupSnowflake()) ?: return

        // To avoid spam, we limit auto messages to only occur during mentions
        if (message.hasMention && info is TimeResult.Info) {
            message.createTimezoneMessage(info, user = message.author)
        } else {
            createTimezoneReaction()
        }
    }

    private val Message.hasMention: Boolean
        get() {
            // Do not accept user mentions if from replies
            if (mentionedUserIds.size >= 2) return type != MessageType.Reply
            return mentionedRoleIds.isNotEmpty() || mentionsEveryone
        }

    private suspend fun Message.createTimezoneMessage(info: TimeResult.Info, user: User?) {
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

    private suspend fun Message.createSignupMessage(groupSnowflake: Snowflake) {
        val authorId = author?.id ?: return
        val prefix = prefixSupplier.prefix(groupSnowflake)
        timeConfigBot.timezoneSignup(authorId, prefix, channel)
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
        val groupSnowflake = message.groupSnowflake(guildId)
        val info = message.timeBotInfo(groupSnowflake) ?: return true
        logger.atFine().log("Sending reaction response message")
        when (info) {
            is TimeResult.Info -> message.createTimezoneMessage(info, user = user)
            is TimeResult.MissingTimezone -> message.createSignupMessage(groupSnowflake)
        }
        return true
    }
}
