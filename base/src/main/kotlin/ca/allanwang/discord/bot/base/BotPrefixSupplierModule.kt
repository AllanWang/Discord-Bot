package ca.allanwang.discord.bot.base

import ca.allanwang.discord.bot.core.BotFeature
import ca.allanwang.discord.bot.firebase.FirebaseModule
import ca.allanwang.discord.bot.firebase.listen
import com.gitlab.kordlib.core.Kord
import com.google.firebase.database.FirebaseDatabase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

interface BotPrefixSupplier {
    fun prefix(): String
}

class BotPrefixSupplierImpl @Inject constructor(
    kord: Kord,
    firebaseDatabase: FirebaseDatabase
) : BotPrefixSupplier {

    companion object {
        private const val PREFIX = "prefix"
    }

    private val atomicPrefix = AtomicReference("!")

    init {
        kord.launch {
            firebaseDatabase.reference.child(kord.selfId.value).child(PREFIX)
                .listen<String>()
                .filterNotNull()
                .collect {
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
