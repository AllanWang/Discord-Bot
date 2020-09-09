package ca.allanwang.discord.bot.firebase

import com.google.common.flogger.FluentLogger
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/*
 * Referenced https://stackoverflow.com/questions/57834950/how-to-handle-callback-using-kotlin-coroutines
 */

 val _firebase_coroutine_logger: FluentLogger = FluentLogger.forEnclosingClass()

suspend inline fun DatabaseReference.singleSnapshot(): DataSnapshot = withContext(Dispatchers.IO) {
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
    try {
        singleSnapshot().getValue(T::class.java)
    } catch (e: Exception) {
        _firebase_coroutine_logger.atWarning().withCause(e).log("firebase single")
        null
    }
}

suspend inline fun DatabaseReference.listenSnapshot(): Flow<DataSnapshot> = withContext(Dispatchers.IO) {
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
        try {
            it.getValue(T::class.java)
        } catch (e: Exception) {
            _firebase_coroutine_logger.atWarning().withCause(e).log("firebase listen")
            null
        }
    }
}