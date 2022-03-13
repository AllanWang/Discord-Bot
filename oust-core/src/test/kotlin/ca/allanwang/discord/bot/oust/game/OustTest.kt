package ca.allanwang.discord.bot.oust.game

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class OustTest {

    @Test
    fun gameCreate() {
        val players = (1..5).map { OustPlayer.Info(id = it.toString(), name = it.toString()) }

        val game = OustGame.create(players)

        println(game)

        assertThat(players.map { it.id }.toSet()).isEqualTo(game.players.map { it.info.id }.toSet())
    }
}
