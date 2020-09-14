package ca.allanwang.discord.bot.base

import ca.allanwang.discord.bot.core.BotFeature
import ca.allanwang.discord.bot.firebase.FirebaseModule
import ca.allanwang.discord.bot.firebase.FirebaseRootRef
import ca.allanwang.discord.bot.firebase.setValue
import com.gitlab.kordlib.core.Kord
import com.google.common.flogger.FluentLogger
import com.google.firebase.database.DatabaseReference
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartBot @Inject constructor(
    @FirebaseRootRef rootRef: DatabaseReference
) : BotFeature {

    companion object {
        private const val NAME = "name"
        private val logger = FluentLogger.forEnclosingClass()
    }

    private val ref: DatabaseReference = rootRef.child(NAME)

    override suspend fun Kord.attach() {
        if (!ref.setValue(getSelf().tag)) {
            logger.atWarning().log("Couldn't update self name on firebase")
        }
    }
}

@Module(includes = [FirebaseModule::class])
object StartBotModule {
    @Provides
    @IntoSet
    @Singleton
    fun bot(bot: StartBot): BotFeature = bot
}