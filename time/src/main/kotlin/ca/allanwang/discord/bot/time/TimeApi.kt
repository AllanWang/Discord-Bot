package ca.allanwang.discord.bot.time

import ca.allanwang.discord.bot.firebase.setValue
import ca.allanwang.discord.bot.firebase.single
import com.gitlab.kordlib.common.entity.Snowflake
import com.google.common.flogger.FluentLogger
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeApi @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase
) {

    val logger = FluentLogger.forEnclosingClass()

    private val timeRef: DatabaseReference
        get() = firebaseDatabase.reference.child("time")

    suspend fun getTime(id: Snowflake): String? =
        timeRef.child(id.value).single<String>()

    suspend fun saveTime(id: Snowflake, value: String): Boolean =
        timeRef.child(id.value).setValue(value)
}