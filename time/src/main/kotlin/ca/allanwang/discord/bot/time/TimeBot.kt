package ca.allanwang.discord.bot.time

import ca.allanwang.discord.bot.core.BotFeature
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.on
import com.google.common.flogger.FluentLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeBot @Inject constructor(
    private val timeApi: TimeApi
) : BotFeature {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()

        val timeRegex = Regex("(1[0-2]|0?[1-9])(?::([0-5][0-9]))?\\s*([AaPp][Mm])?")
    }

    override suspend fun Kord.attach() {
        on<MessageCreateEvent> {
            if (message.author?.isBot == true) return@on

        }
    }
}
