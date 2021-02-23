package ca.allanwang.discord.bot.random

import ca.allanwang.discord.bot.base.CommandHandlerBot
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
object RandomBotModule {
    @Provides
    @IntoSet
    @Singleton
    fun coinBot(bot: CoinBot): CommandHandlerBot = bot

    @Provides
    @IntoSet
    @Singleton
    fun diceBot(bot: DiceBot): CommandHandlerBot = bot
}
