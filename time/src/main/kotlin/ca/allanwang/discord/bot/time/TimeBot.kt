package ca.allanwang.discord.bot.time

import ca.allanwang.discord.bot.base.CommandHandler
import ca.allanwang.discord.bot.base.CommandHandlerBot
import ca.allanwang.discord.bot.base.CommandHandlerEvent
import ca.allanwang.discord.bot.base.commandBuilder
import ca.allanwang.discord.bot.firebase.FirebaseModule
import ca.allanwang.discord.bot.maps.MapsApi
import ca.allanwang.discord.bot.maps.MapsModule
import com.gitlab.kordlib.core.behavior.channel.createEmbed
import com.gitlab.kordlib.rest.builder.message.EmbedBuilder
import com.google.common.flogger.FluentLogger
import com.google.maps.model.AddressType
import com.google.maps.model.GeocodingResult
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import java.awt.Color
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeBot @Inject constructor(
    private val timeApi: TimeApi,
    private val mapApi: MapsApi
) : CommandHandlerBot {

    private val logger = FluentLogger.forEnclosingClass()
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    private val embedColor = Color.decode("#03a5fc")

    override val handler = commandBuilder(CommandHandler.Type.Prefix) {
        arg("timezone") {
            action(withMessage = true) {
                logger.atInfo().log("action")
                if (message.isBlank()) getTimezone()
                else setTimezone(message)
            }
        }
    }

    private suspend fun CommandHandlerEvent.getTimezone() {
        logger.atFine().log()
        val authorId = authorId ?: return
        val timezone = timeApi.getTime(authorId)?.let { TimeZone.getTimeZone(it) }
        if (timezone == null) channel.createMessage("No timezone set; use `$command [city]`")
        else channel.createEmbed {
            color = embedColor
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
        return ":clock${hourString}${minuteString}:"
    }

    private suspend fun getGeocode(query: String): GeocodingResult? = mapApi.getGeocode(query).firstOrNull {
        it.types.contains(AddressType.LOCALITY)
    }

    private fun EmbedBuilder.addTimezoneInfo(timeZone: TimeZone) {
        field {
            name = buildString {
                append(timeZone.worldEmoji())
                append(' ')
                append("TimeZone")
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
            value = dateTime.format(dateTimeFormatter)
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

        if (!timeApi.saveTime(authorId, result.id))
            return failure("Failed to save timezone")

        channel.createEmbed {
            color = embedColor
            title = "Timezone Set"
            addTimezoneInfo(result)
        }
    }
}

@Module(includes = [FirebaseModule::class, MapsModule::class])
object TimeBotModule {
    @Provides
    @IntoSet
    @Singleton
    fun bot(bot: TimeBot): CommandHandlerBot = bot
}