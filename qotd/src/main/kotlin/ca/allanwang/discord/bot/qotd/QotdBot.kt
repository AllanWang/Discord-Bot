package ca.allanwang.discord.bot.qotd

import ca.allanwang.discord.bot.base.*
import ca.allanwang.discord.bot.firebase.FirebaseCache
import ca.allanwang.discord.bot.time.TimeApi
import ca.allanwang.discord.bot.time.TimeConfigBot
import com.google.common.flogger.FluentLogger
import dev.kord.common.Color
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

    override val embedColor: Color = qotd.embedColor

    override val handler =
        commandBuilder("qotd", CommandHandler.Type.Prefix, description = "Question/Quote of the Day") {
            arg("init") {
                action(
                    withMessage = false,
                    help = {
                        buildString {
                            append("Admin only. ")
                            append("Set the main channel for QOTD commands. ")
                            append("Other admin only commands are only valid in the main channel.")
                        }
                    }
                ) {
                    init()
                }
            }
            arg("status") {
                action(withMessage = false, help = { "Admin only. View QOTD Status." }) {
                    status()
                }
            }
            arg("addQuestion") {
                action(
                    withMessage = true,
                    help = {
                        buildString {
                            append("Admin only. ")
                            append("Adds a new question for QOTD. ")
                            append("Questions are sent in order, and each question is deleted after one use.")
                        }
                    }
                ) {
                    addQuestion()
                }
            }
            arg("questions") {
                action(withMessage = false, help = { "Admin only. Show current question pool." }) {
                    questions()
                }
            }
            arg("deleteQuestion") {
                action(
                    withMessage = true,
                    help = {
                        buildString {
                            append("Admin only. ")
                            append("List questions with ids. Pass id to delete a specific question.")
                        }
                    }
                ) {
                    deleteQuestion()
                }
            }
            arg("sample") {
                action(withMessage = false, help = { "Admin only. Show sample QOTD with all configurations." }) {
                    sample()
                }
            }
            arg("now") {
                action(withMessage = false, help = { "Admin only. Send QOTD now." }) {
                    now()
                }
            }
            configCommands()
        }

    private fun CommandBuilderArgDsl.configCommands() {

        fun CommandBuilderArgDsl.removableAction(
            helpArgs: String? = null,
            help: HelpSupplier,
            helpRemove: String,
            block: suspend CommandHandlerEvent.(remove: Boolean) -> Unit
        ) {
            arg(REMOVE_KEY) {
                autoGenHelp = false
                action(withMessage = false) { block(true) }
            }
            action(
                withMessage = true, helpArgs = helpArgs,
                help = {
                    buildString {
                        append(help())
                        append(" Send ")
                        appendCodeBlock { append(REMOVE_KEY) }
                        append(" to ")
                        append(helpRemove)
                    }
                }
            ) {
                block(false)
            }
        }

        arg("channel") {
            removableAction(
                help = { "Set the channel where QOTD will send messages for everyone to see." },
                helpRemove = "disable QOTD."
            ) {
                channel(it)
            }
        }
        arg("image") {
            removableAction(
                help = { "Provide image url to attach to question." },
                helpRemove = "remove image."
            ) {
                image(it)
            }
        }
        arg("template") {
            removableAction(
                help = {
                    buildString {
                        append("Add a template for QOTD. ")
                        append(" Add ")
                        appendCodeBlock { append(Qotd.QUESTION_PLACEHOLDER) }
                        append("in the template, which will be replaced by the selected question.")
                    }
                },
                helpRemove = "remove template."
            ) {
                template(it)
            }
        }
        arg("time") {
            removableAction(
                help = {
                    buildString {
                        append("Set a time when QOTD will start its questions. ")
                        append("Please make sure you've set your timezone (")
                        append(timeConfigBot.timezoneCommand(prefix))
                        append(").")
                    }
                },
                helpRemove = "disable QOTD."
            ) {
                time(it)
            }
        }
        arg("timeInterval") {
            action(withMessage = true, help = { "Set number of hours to wait between QOTD messages." }) {
                timeInterval()
            }
        }
        arg("roleMention") {
            removableAction(
                help = { "Add role to mention with each QOTD." },
                helpRemove = "remove mentions."
            ) {
                roleMention(it)
            }
        }
    }

    /**
     * Returns the guildId if the current channel is associated with the corresponding statusChannel.
     * Used to disable access to QOTD config commands.
     */
    private suspend fun CommandHandlerEvent.statusGuildId(): Snowflake? =
        event.guildId?.takeIf { statusChannelCache.get(it) == channel.id }

    private fun EmbedBuilder.baseQotd() {
        color = qotd.embedColor
        title = "QOTD"
    }

    private suspend fun MessageChannelBehavior.createQotd(builder: EmbedBuilder.() -> Unit) = createEmbed {
        baseQotd()
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
        }
        handler.handleHelp(this)
    }

    private suspend fun CommandHandlerEvent.status() {
        val guildId = statusGuildId() ?: return
        val status = qotd.status(guildId)
        if (status == null) {
            channel.createQotd {
                description = "QOTD not set up"
            }
            handler.handleHelp(this)
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
            channel.createQotd {
                description = "QOTD not set up"
            }
            handler.handleHelp(this)
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
            }
            commandHelp.handleHelp(this)
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
            timeConfigBot.timezoneSignup(this)
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
            baseQotd()
            description = it
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
