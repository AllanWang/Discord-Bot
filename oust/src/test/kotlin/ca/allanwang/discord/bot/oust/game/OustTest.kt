package ca.allanwang.discord.bot.oust.game

import com.gitlab.kordlib.common.entity.Snowflake
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class OustTest {

    @Test
    fun gameCreate() {
        val players = (1..5).map { OustPlayer.Info(id = Snowflake(it.toLong()), name = it.toString()) }

        val game = OustGame.create(players)

        println(game)

        assertThat("Id set match", players.toSet(), equalTo(game.players.map { it.id }.toSet()))
    }

}
