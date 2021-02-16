package ca.allanwang.discord.bot.qotd

import ca.allanwang.discord.bot.firebase.*
import com.google.common.flogger.FluentLogger
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import dev.kord.common.entity.Snowflake
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QotdApi @Inject constructor(
    @FirebaseRootRef rootRef: DatabaseReference
) {

    companion object {
        private const val QOTD = "qotd"

        // High level categories
        private const val CORE = "core"
        private const val FORMAT = "format"
        private const val QUESTIONS = "questions"
        private const val QUESTIONS_ARCHIVE = "questions_archive"

        private const val IMAGE = "image"
        private const val TEMPLATE = "template"
        private const val OUTPUT_CHANNEL = "output_channel"
        private const val STATUS_CHANNEL = "status_channel"
        private const val ROLE_MENTION = "role_mention"
        private const val TIME = "time"
        private const val TIME_INTERVAL = "time_interval"

        internal val DEFAULT_TIME_INTERVAL: Long = TimeUnit.DAYS.toMillis(1)

        private val logger = FluentLogger.forEnclosingClass()
    }

    data class CoreSnapshot(
        val group: Snowflake,
        val statusChannel: Snowflake,
        val outputChannel: Snowflake?,
        val timeInterval: Long,
        val time: Long?,
    ) {
        val isReady: Boolean get() = outputChannel != null && time != null
    }

    data class FormatSnapshot(
        val group: Snowflake,
        val image: String?,
        val template: String?,
        val roleMention: Snowflake?
    )

    private val ref: DatabaseReference
    private val formatRef: DatabaseReference
    private val questionRef: DatabaseReference
    private val questionArchiveRef: DatabaseReference

    init {
        val qotdRef = rootRef.child(QOTD)
        ref = qotdRef.child(CORE)
        formatRef = qotdRef.child(FORMAT)
        questionRef = qotdRef.child(QUESTIONS)
        questionArchiveRef = qotdRef.child(QUESTIONS_ARCHIVE)
    }

    /*
     * Ref based fields which dictates whether qotd should occur
     */

    suspend fun outputChannel(group: Snowflake, channel: Snowflake?) =
        ref.child(group.asString).child(OUTPUT_CHANNEL).setValue(channel?.value)

    suspend fun statusChannel(group: Snowflake, channel: Snowflake) =
        ref.child(group.asString).child(STATUS_CHANNEL).setValue(channel.value)

    suspend fun statusChannel(group: Snowflake): Snowflake? =
        ref.child(group.asString).child(STATUS_CHANNEL).single<Long>()?.let { Snowflake(it) }

    suspend fun time(group: Snowflake, time: Long?) =
        ref.child(group.asString).child(TIME).setValue(time)

    suspend fun timeInterval(group: Snowflake, timeInterval: Long?) =
        ref.child(group.asString).child(TIME_INTERVAL).setValue(timeInterval)
    /*
     * Format ref based fields which dictate how the message is formatted and sent
     */

    suspend fun image(group: Snowflake, url: String?) =
        formatRef.child(group.asString).child(IMAGE).setValue(url)

    suspend fun template(group: Snowflake, template: String?) =
        formatRef.child(group.asString).child(TEMPLATE).setValue(template)

    suspend fun roleMention(group: Snowflake, roleMention: Snowflake?) =
        formatRef.child(group.asString).child(ROLE_MENTION).setValue(roleMention?.value)

    /*
     * Question ref based fields for questions
     */

    suspend fun addQuestion(group: Snowflake, question: String) =
        questionRef.child(group.asString).push().setValue(question)

    suspend fun removeQuestion(group: Snowflake, questionKey: String) =
        questionRef.child(group.asString).child(questionKey).setValue(null)

    suspend fun getQuestion(group: Snowflake, delete: Boolean = true): String? {
        val snapshot = questionRef.child(group.asString).orderByKey().limitToFirst(1).singleSnapshot()
        val questionSnapshot = snapshot.children.firstOrNull()
        val question = questionSnapshot?.getValueOrNull<String>() ?: return null
        if (delete) {
            removeQuestion(group, questionSnapshot.key)
            questionArchiveRef.child(group.asString).push().setValue(question)
        }
        return question
    }

    suspend fun questions(group: Snowflake): Map<String, String> {
        val snapshot = questionRef.child(group.asString).singleSnapshot()
        return snapshot.children.mapNotNull {
            val value = it.getValueOrNull<String>() ?: return@mapNotNull null
            it.key to value
        }.toMap()
    }

    suspend fun coreSnapshot(group: Snowflake): CoreSnapshot? =
        ref.child(group.asString).singleSnapshot().coreSnapshot(group)

    suspend fun coreSnapshotAll(): Set<CoreSnapshot> =
        ref.singleSnapshot().children.mapNotNull { it.coreSnapshot(Snowflake(it.key)) }.toSet()

    private suspend fun DataSnapshot.coreSnapshot(group: Snowflake): CoreSnapshot? {
        val statusChannel = child(STATUS_CHANNEL).getValueOrNull<Long>()?.let { Snowflake(it) } ?: return null
        val time = child(TIME).getValueOrNull<Long>()
        val outputChannel = child(OUTPUT_CHANNEL).getValueOrNull<Long>()?.let { Snowflake(it) }
        val timeInterval = child(TIME_INTERVAL).getValueOrNull<Long>() ?: DEFAULT_TIME_INTERVAL
        return CoreSnapshot(
            group = group,
            time = time,
            timeInterval = timeInterval,
            outputChannel = outputChannel,
            statusChannel = statusChannel
        )
    }

    suspend fun formatSnapshot(group: Snowflake): FormatSnapshot =
        formatRef.child(group.asString).singleSnapshot().formatSnapshot(group)

    private suspend fun DataSnapshot.formatSnapshot(group: Snowflake): FormatSnapshot {
        val image = child(IMAGE).getValueOrNull<String>()
        val template = child(TEMPLATE).getValueOrNull<String>()
        val roleMention = child(ROLE_MENTION).getValueOrNull<Long>()?.let { Snowflake(it) }
        return FormatSnapshot(
            group = group,
            image = image,
            template = template,
            roleMention = roleMention,
        )
    }
}
