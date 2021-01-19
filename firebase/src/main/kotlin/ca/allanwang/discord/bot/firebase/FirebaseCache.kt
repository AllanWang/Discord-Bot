package ca.allanwang.discord.bot.firebase

import java.util.concurrent.ConcurrentHashMap

class FirebaseCache<T, U>(
    private val cacheDuration: Long,
    private val supplier: suspend (T) -> U
) {
    private val cache: ConcurrentHashMap<T, U> = ConcurrentHashMap()
    private val expiration: ConcurrentHashMap<T, Long> = ConcurrentHashMap()

    suspend fun get(key: T): U? {
        val cachedExpiration = expiration[key]
        val cachedValue = cache[key]
        if (cachedExpiration != null && cachedExpiration > System.currentTimeMillis()) return cachedValue
        val newValue = supplier(key)
        expiration[key] = System.currentTimeMillis() + cacheDuration
        if (newValue == null) cache.remove(key)
        else cache[key] = newValue
        return newValue
    }

    fun removeCache(key: T) {
        expiration.remove(key)
        cache.remove(key)
    }
}