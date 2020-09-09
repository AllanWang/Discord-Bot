package ca.allanwang.discord.bot.time

import ca.allanwang.discord.bot.core.BotFeature
import ca.allanwang.discord.bot.firebase.FirebaseModule
import ca.allanwang.discord.bot.maps.MapsApi
import ca.allanwang.discord.bot.maps.MapsModule
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.behavior.channel.createEmbed
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
    private val timeApi: TimeApi,
    private val mapApi: MapsApi
) : BotFeature {

    val logger = FluentLogger.forEnclosingClass()

    override suspend fun Kord.attach() {
        on<MessageCreateEvent> {
            if (message.author?.isBot == true) return@on
            when {
                message.content == "!time" ->
                    getTimezone()
                message.content.startsWith("!loc") ->
                    setTimezone(message.content.substringAfter(' '))
            }
        }
    }

    private suspend fun MessageCreateEvent.getTimezone() {

    }

    private suspend fun MessageCreateEvent.setTimezone(query: String) {

        fun failure() {

        }

        logger.atInfo().log("Query %s", query)
        val result = mapApi.getTimezone(query) ?: return

        message.channel.createEmbed {
            title = "Set Timezone"
            description = "Setting timezone to ${result.displayName}"
        }
        message.channel.createMessage("Results ${result.size} ${result.joinToString("\n\n")}")
    }
}

@Module(includes = [FirebaseModule::class, MapsModule::class])
object TimeBotModule {
    @Provides
    @IntoSet
    @Singleton
    fun botFeature(timebot: TimeBot): BotFeature = timebot
}