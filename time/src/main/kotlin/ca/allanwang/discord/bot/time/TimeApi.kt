package ca.allanwang.discord.bot.time

import ca.allanwang.discord.bot.firebase.setValue
import ca.allanwang.discord.bot.firebase.single
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

    suspend fun getTime(id: Int): Int? {
        return timeRef.child(id.toString()).single<Int>()
    }

    suspend fun saveTime(id: Int, key: Int) {
        timeRef.child(id.toString()).setValue(key)
    }
}