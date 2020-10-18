package ca.allanwang.discord.bot.qotd

import ca.allanwang.discord.bot.base.*
import ca.allanwang.discord.bot.firebase.FirebaseCache
import com.gitlab.kordlib.common.entity.Snowflake
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.behavior.channel.createEmbed
import com.gitlab.kordlib.rest.builder.message.EmbedBuilder
import com.google.common.flogger.FluentLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QotdBot @Inject constructor(
    private val qotd: Qotd,
    private val api: QotdApi
) : CommandHandlerBot {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
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
            configCommands()
        }
    }

    private fun CommandBuilderRootDsl.configCommands() {
        arg("channel") {
            action(withMessage = true) {
                logger.atInfo().log("%s", event.message.mentionedChannelIds)
            }
        }
    }

    /**
     * Returns the guildId if the current channel is associated with the corresponding statusChannel.
     * Used to disable access to QOTD config commands.
     */
    private suspend fun CommandHandlerEvent.statusGuildId(): Snowflake? =
        event.guildId?.takeIf { statusChannelCache.get(it) == channel.id }


    private suspend fun CommandHandlerEvent.init() {
        val guildId = event.guildId
        if (guildId == null) {
            channel.createMessage("QOTD must be used in a server")
            return
        }
        api.statusChannel(guildId, channel.id)

        fun StringBuilder.appendCommand(command: String) = appendCodeBlock {
            append(prefix)
            append("qotd ")
            append(command)
        }

        channel.createEmbed {
            color = qotd.embedColor
            title = "QOTD"
            description = "Welcome to QOTD! All setup and status updates will be sent to this channel"
            field {
                name = "Commands"
            }
            field {
                name = "Configurations"
                value = buildString {
                    appendCommand("config")
                    append(": opens options to format question templates, mentions, and more.")
                }
            }
            field {
                name = "Questions"
                value = buildString {
                    appendCommand("addQuestion [question]")
                    append(": adds a new question for QOTD. If the question pool isn't empty, a random one will be used for a QOTD. Every question is deleted after one use.")
                    appendLine()
                    appendCommand("questions")
                    append(": shows current question pool.")
                    appendLine()
                    appendCommand("sample")
                    append(": shows a sample QOTD")
                }
            }
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
                appendCommand("questions", "Shows current question pool.")
                appendCommand("sample", "Shows a sample QOTD with all the configurations.")
            }.trim()
        }

        field {
            name = "Configuration Commands"
            value = buildString {
                appendCommand("channel", "Set the channel where QOTD will send messages for everyone to see.")
                appendCommand(
                    "time",
                    "Set a time when QOTD will start its questions. Please make sure you've set your timezone (see `${prefix}timezone help`)."
                )
                appendCommand("timeInterval", "Set how wait time between QOTD messages.")

            }.trim()
        }
    }

    private suspend fun CommandHandlerEvent.sample() {
        val guildId = statusGuildId() ?: return
        qotd.qotdSample(channel, guildId)
    }

    private suspend fun CommandHandlerEvent.config() {
        val guildId = statusGuildId() ?: return

    }

    private suspend fun CommandHandlerEvent.questions() {
        val guildId = statusGuildId() ?: return
    }

    private suspend fun CommandHandlerEvent.addQuestion() {
        val guildId = statusGuildId() ?: return
    }

}