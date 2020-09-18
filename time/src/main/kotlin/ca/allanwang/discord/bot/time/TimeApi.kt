package ca.allanwang.discord.bot.time

import ca.allanwang.discord.bot.firebase.*
import com.gitlab.kordlib.common.entity.Snowflake
import com.gitlab.kordlib.core.entity.ReactionEmoji
import com.gitlab.kordlib.kordx.emoji.DiscordEmoji
import com.gitlab.kordlib.kordx.emoji.Emojis
import com.gitlab.kordlib.kordx.emoji.toReaction
import com.gitlab.kordlib.rest.route.Route
import com.google.common.flogger.FluentLogger
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.awt.Color
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

    val dateTimeFormatterNoAmPm = DateTimeFormatter.ofPattern("h:mm")

    val dateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    val reactionEmoji: ReactionEmoji = Emojis.clock3.toReaction()

    val reactionThresholdTime: Long = 5 * 60 * 1000

    val embedColor: Color = Color.decode("#03a5fc")

    suspend fun getTime(group: Snowflake, id: Snowflake): TimeZone? =
        ref.child(group).child(id).single<String>()?.let { TimeZone.getTimeZone(it) }

    suspend fun saveTime(group: Snowflake, id: Snowflake, value: TimeZone): Boolean =
        ref.child(group).child(id).setValue(value.id)

    suspend fun groupTimes(group: Snowflake): List<TimeZone> {
        return ref.child(group)
            .singleSnapshot().children
            .map { it.getValue(String::class.java) }
            .toSet()
            .mapNotNull { TimeZone.getTimeZone(it) }
            .sortedBy { it.rawOffset }
    }
}