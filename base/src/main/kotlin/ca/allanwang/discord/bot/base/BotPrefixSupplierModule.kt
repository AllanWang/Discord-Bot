package ca.allanwang.discord.bot.base

import ca.allanwang.discord.bot.firebase.FirebaseModule
import com.google.common.flogger.FluentLogger
import dagger.Binds
import dagger.Module
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
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
        private val logger = FluentLogger.forEnclosingClass()
    }

    init {
        kord.launch {
            while (isActive) {
                prefixApi.sync()
                delay(TimeUnit.DAYS.toMillis(1))
            }
        }
    }

    override suspend fun prefix(group: Snowflake): String = prefixApi.getPrefix(group)
}

@Module(includes = [FirebaseModule::class])
interface BotPrefixModule {

    @Binds
    @Singleton
    fun to(boxPrefixSupplier: BotPrefixSupplierImpl): BotPrefixSupplier
}
