package ca.allanwang.discord.bot.time

import ca.allanwang.discord.bot.firebase.single
import com.google.common.flogger.FluentLogger
import com.google.firebase.database.FirebaseDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeApi @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase
) {

    val logger = FluentLogger.forEnclosingClass()

    suspend fun test(): Long {
        return firebaseDatabase.reference.child("time").single<Long>() ?: -1
    }
}