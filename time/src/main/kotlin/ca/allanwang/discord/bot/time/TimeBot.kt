package ca.allanwang.discord.bot.time

import ca.allanwang.discord.bot.core.BotFeature
import ca.allanwang.discord.bot.firebase.FirebaseModule
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.on
import com.google.common.flogger.FluentLogger
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeBot @Inject constructor(
    private val api: TimeApi
) : BotFeature {

    val logger = FluentLogger.forEnclosingClass()

    override suspend fun Kord.attach() {
        on<MessageCreateEvent> {
            if (message.author?.isBot == true) return@on
            if (message.content != "!time") return@on
            message.channel.createMessage("Time is ${api.test()}")
        }
    }
}

@Module(includes = [FirebaseModule::class])
object TimeBotModule {
    @Provides
    @IntoSet
    @Singleton
    fun botFeature(timebot: TimeBot): BotFeature = timebot
}