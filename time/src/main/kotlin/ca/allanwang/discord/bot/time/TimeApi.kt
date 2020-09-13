package ca.allanwang.discord.bot.time

import ca.allanwang.discord.bot.firebase.setValue
import ca.allanwang.discord.bot.firebase.single
import com.gitlab.kordlib.common.entity.Snowflake
import com.google.common.flogger.FluentLogger
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.awt.Color
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeApi @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase
) {

    companion object {
        val logger = FluentLogger.forEnclosingClass()
    }

    val dateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    val embedColor: Color = Color.decode("#03a5fc")

    private val timeRef: DatabaseReference
        get() = firebaseDatabase.reference.child("time")

    suspend fun getTime(id: Snowflake): String? =
        timeRef.child(id.value).single<String>()

    suspend fun saveTime(id: Snowflake, value: String): Boolean =
        timeRef.child(id.value).setValue(value)
}