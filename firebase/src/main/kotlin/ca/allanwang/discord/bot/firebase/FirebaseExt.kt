package ca.allanwang.discord.bot.firebase

import dev.kord.common.entity.Snowflake
import com.google.common.flogger.FluentLogger
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/*
 * Referenced https://stackoverflow.com/questions/57834950/how-to-handle-callback-using-kotlin-coroutines
 */

val _firebase_ext_logger: FluentLogger = FluentLogger.forEnclosingClass()

fun DatabaseReference.child(snowflake: Snowflake) = child(snowflake.asString)

suspend fun DatabaseReference.singleSnapshot(): DataSnapshot = withContext(Dispatchers.IO) {
    suspendCancellableCoroutine { cont ->
        addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                cont.resume(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                cont.cancel(error.toException())
            }
        })
    }
}

suspend inline fun <reified T> DatabaseReference.single(): T? = withContext(Dispatchers.IO) {
    singleSnapshot().getValueOrNull<T>()
}

suspend fun DatabaseReference.listenSnapshot(): Flow<DataSnapshot> = withContext(Dispatchers.IO) {
    callbackFlow<DataSnapshot> {
        val valueListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                offer(dataSnapshot)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                close(databaseError.toException())
            }
        }
        addValueEventListener(valueListener)

        awaitClose { removeEventListener(valueListener) }
    }
}

suspend inline fun <reified T> DatabaseReference.listen(): Flow<T?> = withContext(Dispatchers.IO) {
    listenSnapshot().map {
        it.getValueOrNull<T>()
    }
}

suspend fun DatabaseReference.setValue(value: Any?): Boolean = suspendCoroutine { cont ->
    setValue(value) { error, _ ->
        if (error != null)
            _firebase_ext_logger.atInfo().log("Set failed")
        cont.resume(error == null)
    }
}

inline fun <reified T> DataSnapshot.getValueOrNull(): T? = try {
    getValue(T::class.java)
} catch (e: Exception) {
    _firebase_ext_logger.atWarning().withCause(e).log("get value %s %s", key, T::class.simpleName)
    null
}

suspend fun Query.singleSnapshot(): DataSnapshot = withContext(Dispatchers.IO) {
    suspendCancellableCoroutine { cont ->
        addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                cont.resume(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                cont.cancel(error.toException())
            }
        })
    }
}