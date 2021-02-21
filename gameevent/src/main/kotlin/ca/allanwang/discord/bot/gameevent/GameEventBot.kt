package ca.allanwang.discord.bot.gameevent

import ca.allanwang.discord.bot.base.*
import ca.allanwang.discord.bot.time.TimeApi
import ca.allanwang.discord.bot.time.TimeConfigBot
import com.google.common.flogger.FluentLogger
import dev.kord.common.entity.Snowflake
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameEventBot @Inject internal constructor(
    private val mentions: Mentions,
    private val timeApi: TimeApi,
    private val timeConfigBot: TimeConfigBot,
) : CommandHandlerBot {
    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    override val handler = commandBuilder(CommandHandler.Type.Prefix) {
        arg("gameEvent") {
            arg("create") {
                action(withMessage = true) {
                    createEvent()
                }
            }
        }
    }

    data class GameEvent(
        val authorId: Snowflake,
        val message: String,
        val time: Long?,
        val yes: List<Snowflake>,
        val maybe: List<Snowflake>,
        val no: List<Snowflake>,
    )

    private val gameEvents: ConcurrentHashMap<Snowflake, GameEvent> = ConcurrentHashMap()

    private suspend fun CommandHandlerEvent.createEvent() {
        val authorId = authorId ?: return
        val timeEntry = timeApi.findTimes(message).firstOrNull()
        val time: Long?
        if (timeEntry != null) {
            channel.createMessage(
                buildString {
                    append("If you'd like to show a time footer for everyone's specific zone, please set your timezone using ")
                    append(timeConfigBot.timezoneCommand(prefix))
                }
            )
            val timeZone = timeApi.getTime(event.groupSnowflake(), authorId)
        }
        event.message.embeds
    }

    private suspend fun CommandHandlerEvent.getTime(timeEntry: TimeApi.TimeEntry): Long? {
        val authorId = authorId ?: return null
        val timeZone = timeApi.getTime(event.groupSnowflake(), authorId)
        if (timeZone == null) {
            channel.createMessage(
                buildString {
                    append("If you'd like to show a time footer for everyone's specific zone, please set your timezone using ")
                    append(timeConfigBot.timezoneCommand(prefix))
                }
            )
            return null
        }
        return timeEntry.toZonedDateTime(timeZone.toZoneId()).toEpochSecond()
    }


}