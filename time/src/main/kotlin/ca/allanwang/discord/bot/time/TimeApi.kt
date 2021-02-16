package ca.allanwang.discord.bot.time

import ca.allanwang.discord.bot.base.toReaction
import ca.allanwang.discord.bot.firebase.*
import com.gitlab.kordlib.kordx.emoji.Emojis
import com.google.common.flogger.FluentLogger
import com.google.firebase.database.DatabaseReference
import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.ReactionEmoji
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeApi @Inject constructor(
    @FirebaseRootRef rootRef: DatabaseReference
) {

    companion object {
        private const val TIME = "time"
        private val logger = FluentLogger.forEnclosingClass()

        /**
         * A time regex should match values of the format
         * 8am, 9pm, 8:00, 8:00 am
         *
         * To avoid matching tags, time values should not be surrounded by alphanumeric characters.
         */
        val timeRegex = Regex("(?:^|[^a-zA-Z0-9])(1[0-2]|0?[1-9])(?::([0-5][0-9]))?\\s*([AaPp][Mm])?(?:$|[^a-zA-Z0-9])")
    }

    private val ref = rootRef.child(TIME)

    val dateTimeFormatterNoAmPm: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm")

    val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    val reactionEmoji: ReactionEmoji = Emojis.clock3.toReaction()

    val reactionThresholdTime: Long = 5 * 60 * 1000

    val embedColor: Color = Color(0xFF03a5fc.toInt())

    suspend fun getTime(group: Snowflake, id: Snowflake): TimeZone? =
        ref.child(group).child(id).single<String>()?.let { TimeZone.getTimeZone(it) }

    suspend fun saveTime(group: Snowflake, id: Snowflake, value: TimeZone): Boolean =
        ref.child(group).child(id).setValue(value.id)

    suspend fun groupTimes(group: Snowflake): List<TimeZone> {
        return ref.child(group)
            .singleSnapshot().children
            .asSequence()
            .mapNotNull { it.getValueOrNull<String>() }
            .distinct()
            .mapNotNull { TimeZone.getTimeZone(it) }
            .distinctBy { it.rawOffset }
            .sortedBy { it.rawOffset }
            .toList()
    }

    data class TimeEntry(val hour: Int, val minute: Int, val pm: Boolean?) {
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

    fun findTimes(message: CharSequence): List<TimeEntry> =
        timeRegex
            .findAll(message)
            .mapNotNull { it.toTimeEntry() }
            .distinct()
            .toList()
}
