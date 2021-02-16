package ca.allanwang.discord.bot.base

import ca.allanwang.discord.bot.firebase.FirebaseModule
import com.google.common.flogger.FluentLogger
import dagger.Binds
import dagger.Module
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

interface BotPrefixSupplier {
    suspend fun prefix(group: Snowflake): String
}

class BotPrefixSupplierImpl @Inject constructor(
    kord: Kord,
    private val prefixApi: PrefixApi
) : BotPrefixSupplier {

    companion object {
        private const val DEFAULT_PREFIX = "!"
        private val logger = FluentLogger.forEnclosingClass()
    }

    private val atomicPrefix: AtomicReference<Map<Snowflake, String>> = AtomicReference(emptyMap())

    init {
        kord.launch {
            prefixApi.listen()
                .collect {
                    logger.atInfo().log("Updated prefix %s", it)
                    atomicPrefix.set(it)
                }
        }
    }

    override suspend fun prefix(group: Snowflake): String = atomicPrefix.get()[group] ?: DEFAULT_PREFIX
}

@Module(includes = [FirebaseModule::class])
interface BotPrefixModule {

    @Binds
    @Singleton
    fun to(boxPrefixSupplier: BotPrefixSupplierImpl): BotPrefixSupplier
}
