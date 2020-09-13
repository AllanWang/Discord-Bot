package ca.allanwang.discord.bot.time

import ca.allanwang.discord.bot.firebase.child
import ca.allanwang.discord.bot.firebase.setValue
import ca.allanwang.discord.bot.firebase.single
import ca.allanwang.discord.bot.firebase.singleSnapshot
import com.gitlab.kordlib.common.entity.Snowflake
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
    private val firebaseDatabase: FirebaseDatabase
) {

    companion object {
        val logger = FluentLogger.forEnclosingClass()
    }

    val dateTimeFormatterNoAmPm = DateTimeFormatter.ofPattern("h:mm")

    val dateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    val embedColor: Color = Color.decode("#03a5fc")

    private val timeRef: DatabaseReference
        get() = firebaseDatabase.reference.child("time")

    suspend fun getTime(group: Snowflake, id: Snowflake): TimeZone? =
        timeRef.child(group).child(id).single<String>()?.let { TimeZone.getTimeZone(it) }

    suspend fun saveTime(group: Snowflake, id: Snowflake, value: TimeZone): Boolean =
        timeRef.child(group).child(id).setValue(value.id)

    suspend fun groupTimes(group: Snowflake): List<TimeZone> {
        return timeRef.child(group.value)
            .singleSnapshot().children
            .map { it.getValue(String::class.java) }
            .toSet()
            .mapNotNull { TimeZone.getTimeZone(it) }
            .sortedBy { it.rawOffset }
    }
}