package ca.allanwang.discord.bot.qotd

import ca.allanwang.discord.bot.base.*
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.behavior.channel.MessageChannelBehavior
import com.gitlab.kordlib.core.behavior.channel.createEmbed
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
            arg("config") {
                action(withMessage = false) {
                    config()
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
        }
    }

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

    private suspend fun CommandHandlerEvent.sample() {
        val guildId = event.guildId ?: return
        qotd.qotdSample(channel, guildId)
    }

    private suspend fun CommandHandlerEvent.config() {
        val guildId = event.guildId ?: return
    }

    private fun MessageChannelBehavior.configOptions() {

    }

    private suspend fun CommandHandlerEvent.questions() {

    }

    private suspend fun CommandHandlerEvent.addQuestion() {

    }

}