package ca.allanwang.discord.bot.qotd

import ca.allanwang.discord.bot.base.*
import ca.allanwang.discord.bot.firebase.FirebaseCache
import ca.allanwang.discord.bot.time.TimeApi
import ca.allanwang.discord.bot.time.TimeConfigBot
import com.google.common.flogger.FluentLogger
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.rest.builder.message.EmbedBuilder
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QotdBot @Inject constructor(
    private val mentions: Mentions,
    private val qotd: Qotd,
    private val api: QotdApi,
    private val timeApi: TimeApi,
    private val timeConfigBot: TimeConfigBot,
) : CommandHandlerBot {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()

        private const val REMOVE_KEY = "remove"
    }

    private val statusChannelCache: FirebaseCache<Snowflake, Snowflake?> =
        FirebaseCache(60 * 1000L) {
            api.statusChannel(it)
        }

    override suspend fun Kord.attach() {
        qotd.initLoops()
    }

    override val handler = commandBuilder(CommandHandler.Type.Prefix) {
        arg("qotd") {
            arg("init") {
                action(withMessage = false) {
                    init()
                }
            }
            arg("help") {
                action(withMessage = false) {
                    help()
                }
            }
            arg("status") {
                action(withMessage = false) {
                    status()
                }
            }
            arg("sample") {
                action(withMessage = false) {
                    sample()
                }
            }
            arg("questions") {
                action(withMessage = false) {
                    questions()
                }
            }
            arg("addQuestion") {
                action(withMessage = true) {
                    addQuestion()
                }
            }
            arg("deleteQuestion") {
                action(withMessage = true) {
                    deleteQuestion()
                }
            }
            arg("now") {
                action(withMessage = false) {
                    now()
                }
            }
            configCommands()
        }
    }

    private fun CommandBuilderRootDsl.configCommands() {
        arg("channel") {
            arg(REMOVE_KEY) { action(withMessage = false) { channel(remove = true) } }
            action(withMessage = true) {
                channel()
            }
        }
        arg("image") {
            arg(REMOVE_KEY) { action(withMessage = false) { image(remove = true) } }
            action(withMessage = true) {
                image()
            }
        }
        arg("template") {
            arg(REMOVE_KEY) { action(withMessage = false) { template(remove = true) } }
            action(withMessage = true) {
                template()
            }
        }
        arg("time") {
            arg(REMOVE_KEY) { action(withMessage = false) { time(remove = true) } }
            action(withMessage = true) {
                time()
            }
        }
        arg("timeInterval") {
            action(withMessage = true) {
                timeInterval()
            }
        }
        arg("roleMention") {
            arg(REMOVE_KEY) { action(withMessage = false) { roleMention(remove = true) } }
            action(withMessage = true) {
                roleMention()
            }
        }
    }

    /**
     * Returns the guildId if the current channel is associated with the corresponding statusChannel.
     * Used to disable access to QOTD config commands.
     */
    private suspend fun CommandHandlerEvent.statusGuildId(): Snowflake? =
        event.guildId?.takeIf { statusChannelCache.get(it) == channel.id }

    private suspend fun MessageChannelBehavior.createQotd(builder: EmbedBuilder.() -> Unit) = createEmbed {
        color = qotd.embedColor
        title = "QOTD"
        builder()
    }

    private suspend fun CommandHandlerEvent.init() {
        val guildId = event.guildId
        if (guildId == null) {
            channel.createMessage("QOTD must be used in a server")
            return
        }
        if (event.member?.isOwner() != true) {
            channel.createMessage("Only server admins can setup QOTD")
            return
        }
        api.statusChannel(guildId, channel.id)
        statusChannelCache.removeCache(guildId)

        channel.createQotd {
            description = "Welcome to QOTD! All setup and status updates will be sent to this channel"
            commandFields(prefix)
        }
    }

    private suspend fun CommandHandlerEvent.help() {
        channel.createQotd {
            commandFields(prefix)
        }
    }

    private fun EmbedBuilder.commandFields(prefix: String) {
        fun StringBuilder.appendCommand(command: String, description: String) {
            appendCodeBlock {
                append(prefix)
                append("qotd ")
                append(command)
            }
            append(": ")
            append(description)
            appendLine()
        }

        field {
            name = "Commands"
            value = buildString {
                appendCommand("help", "see this message again.")
                appendCommand(
                    "init",
                    "Set the main channel for QOTD commands. All commands below can only be used in the main channel."
                )
                appendCommand(
                    "addQuestion [question]",
                    "Adds a new question for QOTD. If the question pool isn't empty, a random one will be used for a QOTD. Every question is deleted after one use."
                )
                appendCommand("status", "View QOTD status")
                appendCommand("questions", "Shows current question pool.")
                appendCommand("sample", "Shows a sample QOTD with all the configurations.")
                appendCommand("now", "Send a real QOTD now.")
            }.trim()
        }

        field {
            name = "Configuration Commands"
            value = buildString {
                appendCommand(
                    "channel",
                    "Set the channel where QOTD will send messages for everyone to see. Send `remove` to disable QOTD."
                )
                appendCommand(
                    "time",
                    "Set a time when QOTD will start its questions. Please make sure you've set your timezone (see `${prefix}timezone help`). Send `remove` to disable QOTD."
                )
                appendCommand("timeInterval", "Set number of hours to wait between QOTD messages.")
                appendCommand("image", "Provide image url to attach to question. Send `remove` to remove image.")
                appendCommand("roleMention", "Add role to mention with each QOTD. Send `remove` to remove mentions.")
                appendCommand("template", "Add a template for QOTD. Send `remove` to remove template.")
            }.trim()
        }

        templateFormat()
    }

    private fun EmbedBuilder.templateFormat() {
        field {
            name = "Template Format"
            value = buildString {
                append("Add any text here, along with ")
                appendCodeBlock { append(Qotd.QUESTION_PLACEHOLDER) }
                appendLine(".")
                appendCodeBlock { append(Qotd.QUESTION_PLACEHOLDER) }
                append(" will be replaced by the selected question.")
            }.trim()
        }
    }

    private suspend fun CommandHandlerEvent.status() {
        val guildId = statusGuildId() ?: return
        val status = qotd.status(guildId)
        if (status == null) {
            channel.createEmbed {
                description = "QOTD not set up"
                commandFields(prefix)
            }
            return
        }
        with(status) {
            channel.createQotd {
                description = buildString {
                    appendPlural(questionCount, "question")
                    appendLine()
                    if (hoursRemaining == null) {
                        append("No QOTD planned")
                    } else {
                        append("Next QOTD in ")
                        appendPlural(hoursRemaining.toInt(), "hour")
                    }
                    appendLine()
                    append("Repeats every ")
                    appendPlural(timeIntervalHours.toInt(), "hour")
                    appendLine()
                    timestamp = qotdTime?.let { Instant.ofEpochMilli(it) }
                }.trim()
            }
        }
    }

    private suspend fun CommandHandlerEvent.sample() {
        val guildId = statusGuildId() ?: return
        qotd.qotdSample(channel, guildId)
    }

    private suspend fun CommandHandlerEvent.now() {
        val guildId = statusGuildId() ?: return
        val core = api.coreSnapshot(guildId)
        if (core == null) {
            channel.createEmbed {
                description = "QOTD not set up"
                commandFields(prefix)
            }
            return
        }
        if (!qotd.qotdNow(core)) {
            channel.createMessage("Failed to send QOTD.")
        }
    }

    private suspend fun CommandHandlerEvent.channel(remove: Boolean = false) {
        val guildId = statusGuildId() ?: return
        if (remove) {
            api.outputChannel(guildId, null)
            channel.createMessage("Removed channel and disabled QOTD")
            qotd.qotd(guildId)
            return
        }
        val mentionedChannel =
            mentions.channelMentionRegex.find(message)
                ?.groupValues?.get(1)
                ?.takeIf { it.isNotEmpty() }
                ?.let { Snowflake(it) }
        if (mentionedChannel == null) {
            channel.createMessage("Please mention a channel after the command")
            return
        }
        api.outputChannel(guildId, mentionedChannel)
        channel.createMessage("Future QOTDs will be sent to ${mentions.channelMention(mentionedChannel)}")
        qotd.qotd(guildId)
    }

    private suspend fun CommandHandlerEvent.image(remove: Boolean = false) {
        val guildId = statusGuildId() ?: return
        if (remove) {
            api.image(guildId, null)
            channel.createMessage("Removed image in QOTD")
            return
        }
        val url = message.trim()
        // We will use discord's message as a way of verifying image integrity
        try {
            channel.createQotd {
                image = url
                description = "Added image"
            }
        } catch (e: IllegalStateException) {
            channel.createMessage("Please supply a valid image url.")
            return
        }
        api.image(guildId, url)
    }

    private suspend fun CommandHandlerEvent.template(remove: Boolean = false) {
        val guildId = statusGuildId() ?: return
        if (remove) {
            api.template(guildId, null)
            channel.createMessage("Removed template in QOTD")
            return
        }
        if (!qotd.isValidTemplate(message)) {
            channel.createQotd {
                description = "Invalid template"
                templateFormat()
            }
            return
        }
        api.template(guildId, message)
        channel.createMessage(
            buildString {
                append("Template saved. Use ")
                appendCodeBlock {
                    append(prefix)
                    append("qotd sample")
                }
                append(" to view a sample output.")
            }
        )
    }

    private suspend fun CommandHandlerEvent.time(remove: Boolean = false) {
        val authorId = authorId ?: return
        val guildId = statusGuildId() ?: return
        if (remove) {
            api.time(guildId, null)
            channel.createMessage("Removed time and disabled QOTD")
            qotd.qotd(guildId)
            return
        }
        val timeEntry = timeApi.findTimes(message).firstOrNull()
        if (timeEntry == null) {
            channel.createMessage("Please specify a time to send QOTDs.")
            return
        }
        val origTimezone = timeApi.getTime(guildId, authorId)
        if (origTimezone == null) {
            timeConfigBot.timezoneSignup(null, this)
            return
        }
        val time = timeEntry.toZonedDateTime(origTimezone.toZoneId())
        api.time(guildId, time.toEpochSecond() * 1000)
        channel.createMessage("Saved time and started QOTD.")
        qotd.qotd(guildId)
    }

    private suspend fun CommandHandlerEvent.timeInterval() {
        val guildId = statusGuildId() ?: return
        val hours = message.trim().toLongOrNull()
        if (hours == null || hours < 1) {
            channel.createMessage("Please provide a number representing how many hours to wait between questions.")
            return
        }
        api.timeInterval(guildId, TimeUnit.HOURS.toMillis(hours))
        channel.createMessage(
            buildString {
                append("QOTD will send every ")
                appendPlural(hours.toInt(), "hour")
                append(".")
            }
        )
    }

    private suspend fun CommandHandlerEvent.roleMention(remove: Boolean = false) {
        val guildId = statusGuildId() ?: return
        if (remove) {
            api.roleMention(guildId, null)
            channel.createMessage("Removed role mention in QOTD")
            return
        }
        val roleMention = mentions.roleMentionRegex.find(message)
            ?.groupValues?.get(1)
            ?.takeIf { it.isNotEmpty() }
            ?.let { Snowflake(it) }
        if (roleMention == null) {
            channel.createMessage("Please mention a role after the command")
            return
        }
        api.roleMention(guildId, roleMention)
        channel.createMessage("Role mention saved.")
    }

    private suspend fun CommandHandlerEvent.questions(showKey: Boolean = false) {
        val guildId = statusGuildId() ?: return
        val questions = api.questions(guildId)
        val questionText = questions.entries.mapIndexed { index, (key, q) ->
            buildString {
                appendBold {
                    if (showKey) {
                        append(key)
                        append(':')
                    } else {
                        append(index + 1)
                        append('.')
                    }
                }
                append(' ')
                append(q)
            }
        }
        val questionPages = questionText.chunkedByLength(emptyText = "No questions found")

        channel.paginatedMessage(questionPages) {
            createQotd(it)
        }
    }

    private suspend fun CommandHandlerEvent.deleteQuestion() {
        val guildId = statusGuildId() ?: return
        if (message.isBlank()) {
            questions(showKey = true)
            channel.createMessage(
                buildString {
                    append("Delete a question via ")
                    appendCodeBlock {
                        append(prefix)
                        append("qotd deleteQuestion [key]")
                    }
                }
            )
            return
        }
        val result = api.removeQuestion(guildId, message)
        channel.createMessage(if (result) "Deleted $message" else "Deletion failed")
    }

    private suspend fun CommandHandlerEvent.addQuestion() {
        val guildId = statusGuildId() ?: return
        qotd.qotdSample(channel, guildId, message)
        val confirmationMessage = channel.createMessage("Are you okay with the format above?")
        val confirmed = confirmationMessage.confirmationReaction(event.message.author?.id)
        if (confirmed) {
            api.addQuestion(guildId, message)
            channel.createMessage("Added question.")
        } else {
            channel.createMessage("Request cancelled.")
        }
    }
}
