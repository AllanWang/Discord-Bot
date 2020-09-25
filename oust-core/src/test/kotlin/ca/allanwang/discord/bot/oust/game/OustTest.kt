package ca.allanwang.discord.bot.oust.game

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class OustTest {

    @Test
    fun gameCreate() {
        val players = (1..5).map { OustPlayer.Info(id = it.toString(), name = it.toString()) }

        val game = OustGame.create(players)

        println(game)

        assertThat("Id set match", players.map { it.id }.toSet(), equalTo(game.players.map { it.info.id }.toSet()))
    }

}
