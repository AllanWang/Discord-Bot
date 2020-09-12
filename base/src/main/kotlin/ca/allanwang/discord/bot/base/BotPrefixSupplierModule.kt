package ca.allanwang.discord.bot.base

import ca.allanwang.discord.bot.firebase.FirebaseModule
import com.gitlab.kordlib.core.Kord
import com.google.common.flogger.FluentLogger
import dagger.Binds
import dagger.Module
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

interface BotPrefixSupplier {
    fun prefix(): String
}

class BotPrefixSupplierImpl @Inject constructor(
    kord: Kord,
    private val prefixApi: PrefixApi
) : BotPrefixSupplier {

    companion object {
        private const val PREFIX = "prefix"
        private val logger = FluentLogger.forEnclosingClass()
    }

    private val atomicPrefix = AtomicReference("!")

    init {
        kord.launch {
            prefixApi.listen()
                .collect {
                    logger.atInfo().log("Updated prefix %s", it)
                    atomicPrefix.set(it)
                }
        }
    }

    override fun prefix(): String = atomicPrefix.get()
}

@Module(includes = [FirebaseModule::class])
interface BotPrefixModule {

    @Binds
    @Singleton
    fun to(boxPrefixSupplier: BotPrefixSupplierImpl): BotPrefixSupplier
}
