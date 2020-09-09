package ca.allanwang.discord.bot

import ca.allanwang.discord.bot.core.CoreModule
import com.google.common.flogger.FluentLogger
import dagger.Module
import dagger.Provides
import java.util.*
import javax.inject.Named

@Module(includes = [CoreModule::class])
object BotModule {

    private val logger = FluentLogger.forEnclosingClass()

    @Provides
    @JvmStatic
    @Named("kordTokenPropKey")
    fun kordPropKey(): String = "bot_token"

    @Provides
    @JvmStatic
    @Named("kordToken")
    fun token(
        args: Array<String>,
        @Named("kordTokenPropKey") kordTokenPropKey: String,
        @Named("privProperties") privProperties: Properties
    ): String =
        args.firstOrNull()?.also {
            logger.atInfo().log("Got token from args")
        } ?: privProperties.getProperty(kordTokenPropKey)?.also {
            logger.atInfo().log("Got token from properties")
        } ?: error("missing bot token")

}