package ca.allanwang.discord.bot.cinco.game.features

import ca.allanwang.discord.bot.cinco.CincoScope
import dagger.Module
import dagger.Provides

@Module
object CincoGameFeatureModule {
    @Provides
    @CincoScope
    fun cincoFeature(variant: CincoVariant, cincoAzul: CincoAzul): CincoGameFeature = when (variant) {
        CincoVariant.Azul -> cincoAzul
    }
}
