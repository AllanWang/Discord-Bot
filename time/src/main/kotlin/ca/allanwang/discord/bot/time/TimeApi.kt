package ca.allanwang.discord.bot.time

import ca.allanwang.discord.bot.base.toReaction
import ca.allanwang.discord.bot.firebase.*
import com.gitlab.kordlib.kordx.emoji.Emojis
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.ReactionEmoji
import com.google.common.flogger.FluentLogger
import com.google.firebase.database.DatabaseReference
import dev.kord.common.Color
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
        val logger = FluentLogger.forEnclosingClass()
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
}