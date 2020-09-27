package ca.allanwang.discord.bot.cinco.game.core

import ca.allanwang.discord.bot.cinco.CincoPlayers
import ca.allanwang.discord.bot.cinco.CincoScope
import com.gitlab.kordlib.core.entity.User
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@CincoScope
class CincoPointTracker @Inject constructor(
    @CincoPlayers private val players: Set<User>
) {
    private val points: Map<User, AtomicInteger> = players.associateWith { AtomicInteger() }

    fun addPoints(user: User, delta: Int): Int = points.getValue(user).addAndGet(delta)

    data class Entry(val player: User, val points: Int)

    fun getStandings(): List<Entry> = points.map { Entry(it.key, it.value.get()) }.sortedByDescending { it.points }
}