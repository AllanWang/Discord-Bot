package ca.allanwang.discord.bot.time

import ca.allanwang.discord.bot.base.*
import ca.allanwang.discord.bot.maps.MapsApi
import com.google.common.flogger.FluentLogger
import com.google.maps.model.AddressType
import com.google.maps.model.GeocodingResult
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.rest.builder.message.EmbedBuilder
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeConfigBot @Inject constructor(
    private val timeApi: TimeApi,
    private val mapApi: MapsApi,
    private val mentions: Mentions,
) : CommandHandlerBot {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    override val handler = commandBuilder(CommandHandler.Type.Prefix) {
        arg("timezone") {
            action(withMessage = true, helpArgs = "[city]", help = { "Set timezone to matching [city]" }) {
                logger.atInfo().log("action")
                if (message.isBlank()) getTimezone()
                else setTimezone(message)
            }
        }
    }

    private suspend fun CommandHandlerEvent.getTimezone() {
        logger.atFine().log()
        val authorId = authorId ?: return
        val timezone = timeApi.getTime(event.groupSnowflake(), authorId)
        if (timezone == null) timezoneSignup(this)
        else channel.createEmbed {
            color = timeApi.embedColor
            title = "Saved timezone"
            addTimezoneInfo(timezone)
        }
    }

    private fun TimeZone.worldEmoji(): String {
        val utcOffset = rawOffset / 3600000
        logger.atFine().log("Utf offset %d", utcOffset)
        return when (utcOffset) {
            in -1..4 -> ":earth_africa:"
            in 5..11 -> ":earth_asia:"
            else -> ":earth_americas:"
        }
    }

    private fun LocalDateTime.clockEmoji(): String {
        val hourString = (hour % 12).let { if (it == 0) 12 else it }.toString()
        val minuteString = if (minute in 20..40) "30" else ""
        return ":clock${hourString}$minuteString:"
    }

    private val validTypes =
        listOf(AddressType.LOCALITY, AddressType.ADMINISTRATIVE_AREA_LEVEL_1, AddressType.ADMINISTRATIVE_AREA_LEVEL_2)

    private suspend fun getGeocode(query: String): GeocodingResult? {
        val candidates = mapApi.getGeocode(query)
//        candidates.forEachIndexed { i, it ->
//            logger.atInfo().log("Candidate %d %s %s", i, it.formattedAddress, it.types.contentToString())
//        }

        return candidates.firstOrNull { result ->
            validTypes.any { it in result.types }
        }
    }

    private fun EmbedBuilder.addTimezoneInfo(timeZone: TimeZone) {
        field {
            name = buildString {
                append(timeZone.worldEmoji())
                append(' ')
                append("Timezone")
            }
            value = timeZone.displayName
        }
        field {
            val dateTime = LocalDateTime.now(timeZone.toZoneId())
            name = buildString {
                append(dateTime.clockEmoji())
                append(' ')
                append("Time")
            }
            value = dateTime.format(timeApi.dateTimeFormatter)
        }
    }

    private suspend fun CommandHandlerEvent.setTimezone(query: String) {
        val authorId = authorId ?: return
        suspend fun failure(message: String) {
            channel.createMessage(message)
        }
        logger.atInfo().log("Query %s", query)
        val geocode = getGeocode(query) ?: return failure("Could not find timezone")
        val result = mapApi.getTimezone(geocode.geometry.location)
            ?: return failure("Failed to set timezone")
        logger.atFine().log("Received %s", result.displayName)

        if (!timeApi.saveTime(event.groupSnowflake(), authorId, result))
            return failure("Failed to save timezone")

        channel.createEmbed {
            color = timeApi.embedColor
            title = "Timezone Set"
            addTimezoneInfo(result)
        }
    }

    fun timezoneCommand(prefix: String): String = buildString {
        appendCodeBlock {
            append(prefix)
            append("timezone [city]")
        }
    }

    /**
     * Add timezone sign up message. User defaults to null as CommandHandlers are generally a result of an immediate command call.
     * Mentions are generally for reaction events.
     */
    suspend fun timezoneSignup(commandHandlerEvent: CommandHandlerEvent, userSnowflake: Snowflake? = null) {
        timezoneSignup(commandHandlerEvent.channel, commandHandlerEvent.prefix, userSnowflake)
    }

    suspend fun timezoneSignup(channel: MessageChannelBehavior, prefix: String, userSnowflake: Snowflake?) {
        channel.createEmbed {
            title = "Timezone Signup"

            color = timeApi.embedColor

            description = buildString {
                if (userSnowflake != null) {
                    append(mentions.userMention(userSnowflake))
                    append(" has not set their timezone.")
                } else {
                    append("No timezone set.")
                }
                append(" Please use ")
                append(timezoneCommand(prefix))
            }
        }
    }
}
