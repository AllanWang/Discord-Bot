package ca.allanwang.discord.bot.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull

fun <T> Flow<T>.withTimeout(timeMillis: Long): Flow<T> = flow {
    withTimeoutOrNull(timeMillis) {
        collect {
            emit(it)
        }
    }
}
