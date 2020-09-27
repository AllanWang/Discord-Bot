package ca.allanwang.discord.bot.oust.game

import ca.allanwang.discord.bot.core.CoreModule
import dagger.Binds
import dagger.Module
import dagger.Provides


@Module(includes = [CoreModule::class, OustClientModule::class])
object OustTurnModule {

    @Provides
    @OustScope
    fun turnFactory(
        client: OustClient
    ): OustTurn.Factory = object : OustTurn.Factory {
        override fun get(currentPlayer: OustPlayer, otherPlayers: List<OustPlayer>): OustTurn =
            OustTurn(
                currentPlayer = currentPlayer,
                otherPlayers = otherPlayers,
                client = client
            )
    }
}

@Module
interface OustClientModule {

    @Binds
    @OustScope
    fun to(client: OustDiscordClient): OustClient
}