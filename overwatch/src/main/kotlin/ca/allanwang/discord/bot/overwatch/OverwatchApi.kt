package ca.allanwang.discord.bot.overwatch

import ca.allanwang.discord.bot.firebase.FirebaseRootRef
import ca.allanwang.discord.bot.firebase.child
import ca.allanwang.discord.bot.firebase.setValue
import ca.allanwang.discord.bot.firebase.single
import com.google.common.flogger.FluentLogger
import com.google.firebase.database.DatabaseReference
import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OverwatchApi @Inject constructor(
    @FirebaseRootRef rootRef: DatabaseReference
) {

    companion object {
        private const val OVERWATCH = "overwatch"
        private const val PLAY_OVERWATCH_URL = "https://playoverwatch.com/en-us/career/pc/"
        private const val TABLE_ROW_STAT_ID_PREFIX = "0x0860000000000"
        private val logger = FluentLogger.forEnclosingClass()
    }

    private val ref = rootRef.child(OVERWATCH)

    suspend fun getBattleTag(id: Snowflake): String? =
        ref.child(id).child("tag").single()

    suspend fun getUserData(id: Snowflake): OverwatchUser? =
        ref.child(id).single()

    suspend fun saveUserData(id: Snowflake, data: OverwatchUser) {
        ref.child(id).setValue(data)
    }

    suspend fun getFullUserData(id: Snowflake): OverwatchFullData? {
        val oldData = getUserData(id) ?: return null
        val newData = parseUserData(oldData.tag)
        val delta = if (oldData.isComplete && newData?.isComplete == true) newData - oldData else null
        return OverwatchFullData(
            old = oldData.takeIf { it.isComplete },
            new = newData?.takeIf { it.isComplete },
            delta = delta
        )
    }

    private operator fun OverwatchUser.minus(old: OverwatchUser): OverwatchDelta = OverwatchDelta(
        // Level always goes up prestige occurs after level 100
        level = (level - old.level).let { if (it < 0) it + 100 else it },
        endorsementLevel = endorsementLevel - old.endorsementLevel,
        quickPlay = quickPlay - old.quickPlay,
        competitive = competitive - old.competitive
    )

    private operator fun OverwatchUser.GameStats.minus(old: OverwatchUser.GameStats): OverwatchDelta.GameStatsDelta =
        OverwatchDelta.GameStatsDelta(
            wins = wins - old.wins,
            losses = losses - old.losses,
            winRate = winRate - old.winRate
        )

    private fun <T> missing(text: String): T? {
        logger.atInfo().log(text)
        return null
    }

    private fun Element.tableRow(id: String): String? {
        val el = selectFirst("tr.DataTable-tableRow[data-stat-id=$TABLE_ROW_STAT_ID_PREFIX$id]") ?: return null
        if (el.childrenSize() != 2) return missing("row - two children")
        return el.child(1).ownText()
    }

    private fun Element.level(): Int? =
        selectFirst(".player-level")?.text()?.trim()?.toIntOrNull()

    private fun Element.endorsementLevel(): Int? =
        selectFirst(".endorsement-level")?.text()?.trim()?.toIntOrNull()

    private fun Element.gameStats(winId: String = "3F5", lossId: String = "42E"): OverwatchUser.GameStats? {
        val win = tableRow(winId)?.toIntOrNull() ?: return missing("win")
        val loss = tableRow(lossId)?.toIntOrNull() ?: return missing("loss")
        return OverwatchUser.GameStats(wins = win, losses = loss)
    }

    private fun Element.parseUserData(tag: String): OverwatchUser? {
        val name = selectFirst("h1.header-masthead")?.text() ?: return missing("name")
        val level = level() ?: return missing("level")
        val endorsementLevel = endorsementLevel() ?: return missing("endorsementLevel")
        val portraitUrl = selectFirst("img.player-portrait[src]")?.absUrl("src")
        val quickPlay = getElementById("quickplay")?.gameStats() ?: return missing("quickplay")
        val competitive = getElementById("competitive")?.gameStats() ?: return missing("competitive")
        return OverwatchUser(
            tag = tag,
            name = name,
            portraitUrl = portraitUrl,
            level = level,
            endorsementLevel = endorsementLevel,
            quickPlay = quickPlay,
            competitive = competitive,
        )
    }

    fun userDataUrl(tag: String) = "$PLAY_OVERWATCH_URL${tag.replace('#', '-')}"

    suspend fun parseUserData(tag: String): OverwatchUser? = withContext(Dispatchers.IO) {
        val doc = Jsoup.connect(userDataUrl(tag)).get()
        doc.parseUserData(tag)
    }
}

/**
 * User representation for firebase.
 *
 * Do not add fields here as they will be saved.
 */
data class OverwatchUser(
    val tag: String = "",
    val name: String = "",
    val level: Int = 0,
    val endorsementLevel: Int = 0,
    val portraitUrl: String? = null,
    val quickPlay: GameStats = GameStats(),
    val competitive: GameStats = GameStats(),
) {
    data class GameStats(val wins: Int = 0, val losses: Int = 0)
}

val OverwatchUser.isComplete: Boolean get() = name.isNotBlank()

val OverwatchUser.GameStats.winRate: Float
    get() = when {
        wins == 0 && losses == 0 -> 0f
        else -> wins.toFloat() / (wins + losses).toFloat()
    }

data class OverwatchDelta(
    val level: Int,
    val endorsementLevel: Int,
    val quickPlay: GameStatsDelta,
    val competitive: GameStatsDelta
) {
    data class GameStatsDelta(val wins: Int, val losses: Int, val winRate: Float)
}

data class OverwatchFullData(
    val old: OverwatchUser? = null,
    val new: OverwatchUser? = null,
    val delta: OverwatchDelta? = null
)
