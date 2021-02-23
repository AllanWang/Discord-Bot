package ca.allanwang.discord.bot.gameevent

import ca.allanwang.discord.bot.base.*
import ca.allanwang.discord.bot.core.withTimeout
import ca.allanwang.discord.bot.time.TimeApi
import ca.allanwang.discord.bot.time.TimeConfigBot
import com.google.common.flogger.FluentLogger
import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.edit
import dev.kord.core.entity.ReactionEmoji
import dev.kord.rest.builder.message.EmbedBuilder
import kotlinx.coroutines.flow.*
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class GameEventBot @Inject internal constructor(
    private val mentions: Mentions,
    private val timeApi: TimeApi,
    private val timeConfigBot: TimeConfigBot,
) : CommandHandlerBot {
    companion object {
        private val logger = FluentLogger.forEnclosingClass()

        val embedColor = Color(0xff599E70.toInt())

        private val dayInMs = TimeUnit.DAYS.toMillis(1)

        private val yesEmoji: ReactionEmoji =
            ReactionEmoji.Custom(Snowflake(812899961598640149L), "join_yes", isAnimated = false)
        private val maybeEmoji: ReactionEmoji =
            ReactionEmoji.Custom(Snowflake(812899961796689920L), "join_maybe", isAnimated = false)
        private val noEmoji: ReactionEmoji =
            ReactionEmoji.Custom(Snowflake(812899961586057248L), "join_no", isAnimated = false)
    }

    override suspend fun Kord.attach() {
        logger.atInfo().log("Game event attached")
    }

    override val handler = commandBuilder(CommandHandler.Type.Prefix, color = embedColor) {
        description = "Schedule events with attendance and queues"
        arg("gameEvent") {
            arg("create") {
                action(
                    withMessage = true, helpArgs = "[message]",
                    help = {
                        buildString {
                            append("Create a new event. ")
                            append("If your timezone is set (${timeConfigBot.timezoneCommand(prefix)}), ")
                            append("the bot can parse and add it to the bottom right, where it shows with the proper timezone for everyone. ")
                            append("Events can only be set a day in advance, as dates cannot be parsed. ")
                        }
                    }
                ) {
                    createEvent()
                }
            }
        }
    }

    internal data class GameEvent(
        val authorId: Snowflake,
        val authorTag: String?,
        val message: String,
        val timeMs: Long? = null,
        val yes: List<Snowflake> = emptyList(),
        val maybe: List<Snowflake> = emptyList(),
        val no: List<Snowflake> = emptyList(),
    )

    private sealed class GameDelta {

        abstract val userId: Snowflake

        data class Yes(override val userId: Snowflake) : GameDelta()
        data class Maybe(override val userId: Snowflake) : GameDelta()
        data class No(override val userId: Snowflake) : GameDelta()
    }

    private fun GameEvent.filterUser(id: Snowflake): GameEvent =
        copy(yes = yes.filter { it != id }, maybe = maybe.filter { it != id }, no = no.filter { it != id })

    private fun EmbedBuilder.addGameEvent(gameEvent: GameEvent) {
        color = embedColor
        description = gameEvent.message
        field {
            name = "---------"
        }
        field {
            name = buildString {
                appendReaction(yesEmoji)
                append(" Yes")
            }
            setUsers(gameEvent.yes)
            inline = true
        }
        field {
            name = buildString {
                appendReaction(maybeEmoji)
                append(" Maybe")
            }
            setUsers(gameEvent.maybe)
            inline = true
        }
        field {
            name = buildString {
                appendReaction(noEmoji)
                append(" No")
            }
            setUsers(gameEvent.no)
            inline = true
        }
        if (gameEvent.authorTag != null) {
            footer {
                text = "Created by ${gameEvent.authorTag}"
            }
        }
        if (gameEvent.timeMs != null) {
            timestamp = Instant.ofEpochMilli(gameEvent.timeMs)
        }
    }

    private fun EmbedBuilder.Field.setUsers(users: List<Snowflake>) {
        if (users.isEmpty()) {
            value = "---"
            return
        }
        value = buildString {
            users.forEachIndexed { index, id ->
                appendBold {
                    append(index + 1)
                    append(": ")
                }
                appendLine(mentions.userMention(id))
            }
        }.trim()
    }

    private suspend fun CommandHandlerEvent.createEvent() {
        logger.atInfo().log("Create Game Event Request")
        val baseGameEvent = gameEvent() ?: return
        val expiration = expiration(baseGameEvent.timeMs)
        val message = channel.createEmbed {
            addGameEvent(baseGameEvent)
        }
        message.addReaction(yesEmoji)
        message.addReaction(maybeEmoji)
        message.addReaction(noEmoji)
        message.reactionAddEvents()
            .withTimeout(expiration)
            .mapNotNull {
                when (it.emoji) {
                    yesEmoji -> GameDelta.Yes(it.userId)
                    maybeEmoji -> GameDelta.Maybe(it.userId)
                    noEmoji -> GameDelta.No(it.userId)
                    else -> null
                }
            }
            .scan(baseGameEvent) { gameEvent, delta ->
                val userId = delta.userId
                val newGameEvent = gameEvent.filterUser(userId)
                when (delta) {
                    is GameDelta.Yes -> {
                        message.deleteReaction(userId, maybeEmoji)
                        message.deleteReaction(userId, noEmoji)
                        newGameEvent.copy(yes = newGameEvent.yes + userId)
                    }
                    is GameDelta.Maybe -> {
                        message.deleteReaction(userId, yesEmoji)
                        message.deleteReaction(userId, noEmoji)
                        newGameEvent.copy(maybe = newGameEvent.maybe + userId)
                    }
                    is GameDelta.No -> {
                        message.deleteReaction(userId, yesEmoji)
                        message.deleteReaction(userId, maybeEmoji)
                        newGameEvent.copy(no = newGameEvent.no + userId)
                    }
                }
            }
            // Scan will emit initial event
            .drop(1)
            .distinctUntilChanged()
            // If multiple reactions happen before a message edit, we only care about the end state
            .conflate()
            .collect { gameEvent ->
                logger.atFine().log("New event $gameEvent")
                message.edit {
                    embed { addGameEvent(gameEvent) }
                }
            }
        // Show that event is no longer active
        message.deleteReaction(yesEmoji)
        message.deleteReaction(maybeEmoji)
        message.deleteReaction(noEmoji)
    }

    /**
     * Get delay in ms for when event at epoch should expire.
     * Currently guaranteed to be positive number within 24h
     */
    private fun expiration(epoch: Long?): Long {
        val now = System.currentTimeMillis()
        if (epoch == null) return dayInMs
        if (epoch > now) return min(dayInMs, epoch - now)
        return dayInMs
    }

    private suspend fun CommandHandlerEvent.gameEvent(): GameEvent? {
        val authorId = authorId ?: return null
        val timeEntry = timeApi.findTimes(message).firstOrNull()
        val timeMs: Long? = timeEntry?.let { getTimeMs(it) }
        return GameEvent(
            authorId = authorId,
            authorTag = event.message.author?.tag,
            message = message,
            timeMs = timeMs,
        )
    }

    private suspend fun CommandHandlerEvent.getTimeMs(timeEntry: TimeApi.TimeEntry): Long? {
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
        return timeEntry.toZonedDateTime(timeZone.toZoneId()).toEpochSecond() * 1000
    }
}
