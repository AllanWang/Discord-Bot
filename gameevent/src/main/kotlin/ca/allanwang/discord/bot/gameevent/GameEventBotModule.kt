package ca.allanwang.discord.bot.gameevent

import ca.allanwang.discord.bot.base.CommandHandlerBot
import ca.allanwang.discord.bot.firebase.FirebaseModule
import ca.allanwang.discord.bot.time.TimeBotModule
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module(includes = [FirebaseModule::class, TimeBotModule::class])
object GameEventBotModule {

    @Provides
    @IntoSet
    @Singleton
    fun bot(bot: GameEventBot): CommandHandlerBot = bot
}
