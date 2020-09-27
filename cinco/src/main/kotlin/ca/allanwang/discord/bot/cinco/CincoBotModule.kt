package ca.allanwang.discord.bot.cinco

import ca.allanwang.discord.bot.base.CommandHandlerBot
import ca.allanwang.discord.bot.firebase.FirebaseModule
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module(includes = [FirebaseModule::class, CincoModule::class])
object CincoBotModule {

    @Provides
    @IntoSet
    @Singleton
    fun bot(bot: CincoBot): CommandHandlerBot = bot
}