package ca.allanwang.discord.bot.oust

import ca.allanwang.discord.bot.base.CommandHandlerBot
import ca.allanwang.discord.bot.firebase.FirebaseModule
import ca.allanwang.discord.bot.oust.game.OustScope
import ca.allanwang.discord.bot.oust.game.OustTurn
import ca.allanwang.discord.bot.oust.game.OustTurnDiscord
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module(includes = [FirebaseModule::class, OustModule::class])
object OustBotModule {

    @Provides
    @IntoSet
    @Singleton
    fun bot(bot: OustBot): CommandHandlerBot = bot
}