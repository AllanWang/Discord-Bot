package ca.allanwang.discord.bot.qotd

import ca.allanwang.discord.bot.base.ColorPalette
import ca.allanwang.discord.bot.base.Mentions
import com.google.common.flogger.FluentLogger
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
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class Qotd @Inject constructor(
    private val kord: Kord,
    colorPalette: ColorPalette,
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
            val cyclesPassed = (now - qotdTime) / timeInterval
            val newTime = qotdTime + cyclesPassed * timeInterval
            return if (newTime < now + TIME_THRESHOLD) newTime + timeInterval else newTime
        }

        internal fun newTime(core: QotdApi.CoreSnapshot): Long? {
            if (!core.isReady) return null
            // Should never be null given constraints
            val time = core.time ?: return null
            val timeInterval = core.timeInterval
            return newTime(time, System.currentTimeMillis(), timeInterval)
        }
    }

    private val loopers: ConcurrentHashMap<Snowflake, Job> = ConcurrentHashMap()

    val embedColor = colorPalette.lavendar

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

    data class Status(
        val questionCount: Int,
        val qotdTime: Long?,
        val hoursRemaining: Long?,
        val timeIntervalHours: Long
    )

    suspend fun status(group: Snowflake): Status? {
        val core = api.coreSnapshot(group) ?: return null
        val questionCount = api.questions(group).size
        val qotdTime = newTime(core)
        val hoursRemaining = qotdTime?.let {
            TimeUnit.MILLISECONDS.toHours(it - System.currentTimeMillis())
        }
        val timeIntervalHours = TimeUnit.MILLISECONDS.toHours(core.timeInterval)
        return Status(
            questionCount = questionCount,
            qotdTime = qotdTime,
            hoursRemaining = hoursRemaining,
            timeIntervalHours = timeIntervalHours
        )
    }

    suspend fun qotd(group: Snowflake, refCoreSnapshot: QotdApi.CoreSnapshot? = null) {
        val core = refCoreSnapshot ?: api.coreSnapshot(group)
        if (core?.isReady != true) return clearJob(group)
        val qotdTime = core.time ?: return clearJob(group)
        val now = System.currentTimeMillis()
        // Within threshold of expected qotd; send now
        if (abs(qotdTime - now) < TIME_THRESHOLD) {
            qotdNow(core)
        }
        // Check for new qotd time
        val newQotdTime = when {
            qotdTime > now + TIME_THRESHOLD -> qotdTime
            else -> newTime(core)
        } ?: return
        // Update time and queue
        api.time(group, newQotdTime)
        setJob(group) {
            delay(newQotdTime - now)
            qotd(group)
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
     * Attempts to send a QOTD instantly.
     * Return true if successful.
     */
    suspend fun qotdNow(core: QotdApi.CoreSnapshot): Boolean {
        val channel = core.outputChannel ?: return false
        val question = api.getQuestion(core.group)
        if (question == null) {
            kord.unsafe.guildMessageChannel(core.group, core.statusChannel).createMessage("No more questions for QOTD.")
            return false
        }
        val format = api.formatSnapshot(core.group)
        return try {
            kord.unsafe.guildMessageChannel(core.group, channel).createQotd(format, question)
            true
        } catch (e: RestRequestException) {
            logger.atSevere().withCause(e).log("Could not send QOTD")
            kord.unsafe.guildMessageChannel(core.group, core.statusChannel).createMessage("Could not send QOTD")
            false
        }
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
