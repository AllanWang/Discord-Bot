package ca.allanwang.discord.bot.oust.game

import ca.allanwang.discord.bot.core.CoreModule
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton


@Module(includes = [CoreModule::class, OustClientModule::class])
object OustTurnModule {

    @Provides
    @OustScope
    fun turnFactory(
        client: OustClient
    ): OustTurn.Factory = object : OustTurn.Factory {
        override fun get(currentPlayer: OustPlayer): OustTurn =
            OustTurn(
                currentPlayer = currentPlayer,
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