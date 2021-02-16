package ca.allanwang.discord.bot.time

import ca.allanwang.discord.bot.base.CommandHandlerBot
import ca.allanwang.discord.bot.core.BotFeature
import ca.allanwang.discord.bot.firebase.FirebaseModule
import ca.allanwang.discord.bot.maps.MapsModule
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module(includes = [FirebaseModule::class, MapsModule::class])
object TimeBotModule {
    @Provides
    @IntoSet
    @Singleton
    fun configBot(bot: TimeConfigBot): CommandHandlerBot = bot

    @Provides
    @IntoSet
    @Singleton
    fun bot(bot: TimeBot): BotFeature = bot
}
