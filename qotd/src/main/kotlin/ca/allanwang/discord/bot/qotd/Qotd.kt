package ca.allanwang.discord.bot.qotd

import ca.allanwang.discord.bot.base.MentionRegex
import com.gitlab.kordlib.common.entity.Snowflake
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.behavior.channel.MessageChannelBehavior
import com.gitlab.kordlib.core.behavior.channel.createEmbed
import com.gitlab.kordlib.rest.request.RestRequestException
import com.google.common.flogger.FluentLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Qotd @Inject constructor(
    private val kord: Kord,
    private val mentionRegex: MentionRegex,
    private val api: QotdApi
) {

    companion object {
        private const val QUESTION_PLACEHOLDER = "\$question\$"
        private const val MENTION_PLACEHOLDER = "\$mention\$"

        /**
         * Max time difference in milliseconds between expected qotd request time and current time
         */
        private const val TIME_THRESHOLD = 5000L

        private val logger = FluentLogger.forEnclosingClass()

        /**
         * Return expected time for next qotd.
         * Time is guaranteed to be after [now] by at most [timeInterval]
         */
        internal fun newTime(qotdTime: Long, now: Long, timeInterval: Long): Long {
            val cyclesPassed = (now - qotdTime) / timeInterval + 1
            return qotdTime + cyclesPassed * timeInterval
        }
    }

    private val loopers: ConcurrentHashMap<Snowflake, Job> = ConcurrentHashMap()

    val embedColor = Color.decode("#AB98D2")

    suspend fun initLoops() {
        api.coreSnapshotAll().forEach {
            qotd(it.group, it)
        }
    }

    private fun setJob(group: Snowflake, action: suspend () -> Unit) {
        val oldJob = loopers[group]
        loopers[group] = kord.launch {
            action()
        }
        oldJob?.cancel("Overwritten job")
    }

    private fun clearJob(group: Snowflake) {
        kord.launch {
            loopers[group]?.cancel("Requested cancellation")
            loopers.remove(group)
        }
    }

    suspend fun status(group: Snowflake): String {
        return "TODO"
    }

    suspend fun qotdInit(group: Snowflake, channelBehavior: MessageChannelBehavior) {
        api.statusChannel(group, channelBehavior.id)
        channelBehavior.createEmbed {
            color = embedColor
            title = "QOTD"
            description = "Welcome to QOTD! All setup and status updates will be sent to this channel"
            field {
                name = "Configurations"

            }
        }
    }

    suspend fun qotd(group: Snowflake, refCoreSnapshot: QotdApi.CoreSnapshot? = null) {
        val coreSnapshot = refCoreSnapshot ?: api.coreSnapshot(group)
        val qotdTime = coreSnapshot?.time ?: return clearJob(group)
        val now = System.currentTimeMillis()
        when {
            // Too much time has passed
            qotdTime < now - TIME_THRESHOLD -> return clearJob(group)
            // Okay to launch request
            qotdTime < now + TIME_THRESHOLD -> {
                val newQotdTime = qotdNow(coreSnapshot) ?: return clearJob(group)
                if (newQotdTime > now + TIME_THRESHOLD) setJob(group) {
                    delay(newQotdTime - now)
                    qotd(group)
                }
                return
            }
            // Pending request too far into the future
            else -> {
                setJob(group) {
                    delay(qotdTime - now)
                    qotd(group)
                }
            }
        }
    }

    /**
     * Gets the next QOTD, without modifications.
     *
     * Based on how the next question is fetched, this will match the next QOTD if there are no modifications.
     */
    suspend fun qotdSample(
        channelBehavior: MessageChannelBehavior,
        group: Snowflake,
        question: String = "Hello! This is a sample question"
    ) {
        suspend fun fail(message: String) {
            channelBehavior.createMessage(message)
        }

        val formatSnapshot = api.formatSnapshot(group) ?: return fail("Could not get QOTD formatter")
        channelBehavior.createQotd(formatSnapshot, question)
    }

    private suspend fun qotdNow(data: QotdApi.CoreSnapshot): Long? {
        val formatSnapshot = api.formatSnapshot(data.group) ?: return null
        val question = api.getQuestion(data.group) ?: return null
        val channel = data.outputChannel ?: return null
        try {
            kord.unsafe.guildMessageChannel(data.group, channel).createQotd(formatSnapshot, question)
        } catch (e: RestRequestException) {
            logger.atSevere().withCause(e).log("Could not send qotd")
            kord.unsafe.guildMessageChannel(data.group, data.statusChannel).createMessage("Could not send QOTD")
            return null
        }
        // Should never be null given constraints
        val time = data.time ?: return null
        val timeInterval = formatSnapshot.timeInterval ?: return run {
            logger.atWarning().log("No more timeinterval for %s", data.group.value)
            null
        }
        val newTime = newTime(time, System.currentTimeMillis(), timeInterval)
        api.time(data.group, newTime)
        return newTime
    }

    private suspend fun MessageChannelBehavior.createQotd(formatSnapshot: QotdApi.FormatSnapshot, question: String) {
        createEmbed {
            color = embedColor
            title = "QOTD"
            image = formatSnapshot.image

            val roleMention = formatSnapshot.roleMention?.let { mentionRegex.roleMention(it) } ?: ""

            description = buildString {
                if (formatSnapshot.template == null) {
                    append(question)
                    if (roleMention.isNotEmpty()) {
                        append("\n\n")
                        append(roleMention)
                    }
                } else {
                    append(
                        formatSnapshot.template
                            .replace(QUESTION_PLACEHOLDER, question)
                            .replace(MENTION_PLACEHOLDER, roleMention)
                    )
                }
            }
        }
    }
}