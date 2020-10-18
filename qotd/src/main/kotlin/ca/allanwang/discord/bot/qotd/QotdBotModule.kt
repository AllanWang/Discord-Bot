package ca.allanwang.discord.bot.qotd

import ca.allanwang.discord.bot.base.CommandHandlerBot
import ca.allanwang.discord.bot.firebase.FirebaseModule
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module(includes = [FirebaseModule::class])
object QotdBotModule {

    @Provides
    @IntoSet
    @Singleton
    fun bot(bot: QotdBot): CommandHandlerBot = bot
}