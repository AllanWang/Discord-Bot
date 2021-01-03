package ca.allanwang.discord.bot.qotd

import ca.allanwang.discord.bot.base.Mentions
import com.google.common.flogger.FluentLogger
import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.rest.request.RestRequestException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Qotd @Inject constructor(
    private val kord: Kord,
    private val mentions: Mentions,
    private val api: QotdApi
) {

    companion object {
        internal const val QUESTION_PLACEHOLDER = "\$question\$"

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

        internal fun newTime(core: QotdApi.CoreSnapshot, format: QotdApi.FormatSnapshot): Long? {
            // Should never be null given constraints
            val time = core.time ?: return null
            val timeInterval = format.timeInterval
            return newTime(time, System.currentTimeMillis(), timeInterval)
        }
    }

    private val loopers: ConcurrentHashMap<Snowflake, Job> = ConcurrentHashMap()

    val embedColor = Color(0xFFAB98D2.toInt())

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
        val formatSnapshot = api.formatSnapshot(group)
        channelBehavior.createQotd(formatSnapshot.copy(roleMention = null), question)
    }

    /**
     * Attempts to send a QOTD instantly
     */
    private suspend fun qotdNow(core: QotdApi.CoreSnapshot): Long? {
        val formatSnapshot = api.formatSnapshot(core.group)
        val question = api.getQuestion(core.group) ?: return null
        val channel = core.outputChannel ?: return null
        try {
            kord.unsafe.guildMessageChannel(core.group, channel).createQotd(formatSnapshot, question)
        } catch (e: RestRequestException) {
            logger.atSevere().withCause(e).log("Could not send qotd")
            kord.unsafe.guildMessageChannel(core.group, core.statusChannel).createMessage("Could not send QOTD")
            return null
        }
        // Should never be null given constraints
        val time = core.time ?: return null
        val timeInterval = formatSnapshot.timeInterval
        val newTime = newTime(time, System.currentTimeMillis(), timeInterval)
        api.time(core.group, newTime)
        return newTime
    }

    fun isValidTemplate(template: String): Boolean {
        return template.contains(QUESTION_PLACEHOLDER)
    }

    private suspend fun MessageChannelBehavior.createQotd(
        formatSnapshot: QotdApi.FormatSnapshot,
        question: String
    ) {
        createEmbed {
            color = embedColor
            title = "QOTD"
            image = formatSnapshot.image

            description = buildString {
                if (formatSnapshot.template == null) {
                    append(question)
                } else {
                    append(formatSnapshot.template.replace(QUESTION_PLACEHOLDER, question))
                }
            }
        }
        val roleMention = formatSnapshot.roleMention?.let { mentions.roleMention(it) } ?: return
        createMessage(roleMention)
    }
}