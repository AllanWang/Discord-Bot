package ca.allanwang.discord.bot.cinco.game

import ca.allanwang.discord.bot.cinco.CincoScope
import dagger.Module
import dagger.Provides

@Module
object CincoGameModule {
    @Provides
    @CincoScope
    fun cinco(variant: CincoVariant, cincoAzul: CincoAzul): CincoGame = when (variant) {
        CincoVariant.Azul -> cincoAzul
    }
}