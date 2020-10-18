package ca.allanwang.discord.bot.qotd

import ca.allanwang.discord.bot.firebase.*
import com.gitlab.kordlib.common.entity.Snowflake
import com.google.common.flogger.FluentLogger
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
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

        private const val IMAGE = "image"
        private const val TEMPLATE = "template"
        private const val OUTPUT_CHANNEL = "output_channel"
        private const val STATUS_CHANNEL = "status_channel"
        private const val ROLE_MENTION = "role_mention"
        private const val TIME = "time"
        private const val TIME_INTERVAL = "time_interval"

        private val logger = FluentLogger.forEnclosingClass()
    }

    data class CoreSnapshot(
        val group: Snowflake,
        val statusChannel: Snowflake,
        val outputChannel: Snowflake?,
        val time: Long?,
    )

    data class FormatSnapshot(
        val group: Snowflake,
        val image: String?,
        val template: String?,
        val timeInterval: Long?,
        val roleMention: Snowflake?
    )

    data class Snapshot(
        val group: Snowflake,
        val statusChannel: Snowflake,
        val outputChannel: Snowflake?,
        val image: String?,
        val template: String?,
        val time: Long?,
        val timeInterval: Long?,
    )

    private val ref: DatabaseReference
    private val formatRef: DatabaseReference
    private val questionRef: DatabaseReference

    init {
        val qotdRef = rootRef.child(QOTD)
        ref = qotdRef.child(CORE)
        formatRef = qotdRef.child(FORMAT)
        questionRef = qotdRef.child(QUESTIONS)
    }

    /*
     * Ref based fields which dictates whether qotd should occur
     */

    suspend fun outputChannel(group: Snowflake, channel: Snowflake?) =
        ref.child(group.value).child(OUTPUT_CHANNEL).setValue(channel?.value)

    suspend fun statusChannel(group: Snowflake, channel: Snowflake) =
        ref.child(group.value).child(STATUS_CHANNEL).setValue(channel.value)

    suspend fun statusChannel(group: Snowflake): Snowflake? =
        ref.child(group.value).child(STATUS_CHANNEL).single<String>()?.let { Snowflake(it) }

    suspend fun time(group: Snowflake, time: Long?) =
        ref.child(group.value).child(TIME).setValue(time)

    /*
     * Format ref based fields which dictate how the messsage is formatted and sent
     */

    suspend fun image(group: Snowflake, url: String?) =
        formatRef.child(group.value).child(IMAGE).setValue(url)

    suspend fun template(group: Snowflake, template: String?) =
        formatRef.child(group.value).child(TEMPLATE).setValue(template)

    suspend fun timeInterval(group: Snowflake, timeInterval: Long?) =
        formatRef.child(group.value).child(TIME_INTERVAL).setValue(timeInterval)

    suspend fun roleMention(group: Snowflake, roleMention: String?) =
        formatRef.child(group.value).child(ROLE_MENTION).setValue(roleMention)

    /*
     * Question ref based fields for questions
     */

    suspend fun addQuestion(group: Snowflake, question: String) =
        questionRef.child(group.value).child(UUID.randomUUID().toString()).setValue(question)

    suspend fun removeQuestion(group: Snowflake, questionKey: String) =
        questionRef.child(group.value).child(questionKey).setValue(null)

    suspend fun getQuestion(group: Snowflake, delete: Boolean = true): String? {
        val snapshot = questionRef.child(group.value).orderByKey().limitToFirst(1).singleSnapshot()
        val questionSnapshot = snapshot.children.firstOrNull()
        val question = questionSnapshot?.getValueOrNull<String>() ?: return null
        if (delete) removeQuestion(group, questionSnapshot.key)
        return question
    }

    suspend fun questions(group: Snowflake): Map<String, String> {
        val snapshot = questionRef.child(group.value).singleSnapshot()
        return snapshot.children.mapNotNull {
            val value = it.getValueOrNull<String>() ?: return@mapNotNull null
            it.key to value
        }.toMap()
    }

    suspend fun coreSnapshot(group: Snowflake): CoreSnapshot? =
        ref.child(group.value).singleSnapshot().coreSnapshot(group)

    suspend fun coreSnapshotAll(): Set<CoreSnapshot> =
        ref.singleSnapshot().children.mapNotNull { it.coreSnapshot(Snowflake(it.key)) }.toSet()

    private suspend fun DataSnapshot.coreSnapshot(group: Snowflake): CoreSnapshot? {
        val statusChannel = child(STATUS_CHANNEL).getValueOrNull<String>()?.let { Snowflake(it) } ?: return null
        val time = child(TIME).getValueOrNull<Long>()
        val outputChannel = child(OUTPUT_CHANNEL).getValueOrNull<String>()?.let { Snowflake(it) }
        return CoreSnapshot(
            group = group,
            time = time,
            outputChannel = outputChannel,
            statusChannel = statusChannel
        )
    }

    suspend fun formatSnapshot(group: Snowflake): FormatSnapshot? =
        formatRef.child(group.value).singleSnapshot().formatSnapshot(group)

    private suspend fun DataSnapshot.formatSnapshot(group: Snowflake): FormatSnapshot? {
        val image = child(IMAGE).getValueOrNull<String>()
        val template = child(TEMPLATE).getValueOrNull<String>()
        val timeInterval = child(TIME_INTERVAL).getValueOrNull<Long>()
        val roleMention = child(ROLE_MENTION).getValueOrNull<String>()?.let { Snowflake(it) }
        return FormatSnapshot(
            group = group,
            image = image,
            template = template,
            timeInterval = timeInterval ?: TimeUnit.DAYS.toMillis(1), // Default to one day
            roleMention = roleMention,
        )
    }
}