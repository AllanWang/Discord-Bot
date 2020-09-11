package ca.allanwang.discord.bot.time

import ca.allanwang.discord.bot.core.BotFeature
import ca.allanwang.discord.bot.firebase.FirebaseModule
import ca.allanwang.discord.bot.maps.MapsApi
import ca.allanwang.discord.bot.maps.MapsModule
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.behavior.channel.createEmbed
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.on
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
) : BotFeature {

    private val logger = FluentLogger.forEnclosingClass()
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")


    override suspend fun Kord.attach() {
        on<MessageCreateEvent> {
            if (message.author?.isBot == true) return@on
            when {
                message.content == "!time" ->
                    getTimezone()
                message.content.startsWith("!timezone ") ->
                    setTimezone(message.content.substringAfter(' '))
            }
        }
    }

    private suspend fun MessageCreateEvent.getTimezone() {

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

    suspend fun getTimezone(query: String): TimeZone? {
        val result = getGeocode(query).firstOrNull() ?: return null
        result.addressComponents
        val latLng = result.geometry.location
        return getTimezone(latLng)
    }

    private suspend fun getGeocode(query: String): GeocodingResult? = mapApi.getGeocode(query).firstOrNull {
        it.types.contains(AddressType.LOCALITY)
    }

    private suspend fun MessageCreateEvent.setTimezone(query: String) {

        suspend fun failure() {
            message.channel.createMessage("Failed to set timezone")
        }

        logger.atInfo().log("Query %s", query)
        val geocode = getGeocode(query) ?: return failure()
        val result = mapApi.getTimezone(geocode.geometry.location) ?: return failure()
        logger.atFine().log("Received %s", result.displayName)
        message.channel.createEmbed {
            color = Color.decode("#03a5fc")
            title = "Timezone Set"
            field {
                value = buildString {
                    append(result.worldEmoji())
                    append(' ')
                    append(result.displayName)
                }
            }

            field {
                val dateTime = LocalDateTime.now(result.toZoneId())
                value = buildString {
                    append(dateTime.clockEmoji())
                    append(' ')
                    append(dateTime.format(dateTimeFormatter))
                }
            }
        }
    }
}

@Module(includes = [FirebaseModule::class, MapsModule::class])
object TimeBotModule {
    @Provides
    @IntoSet
    @Singleton
    fun botFeature(timebot: TimeBot): BotFeature = timebot
}