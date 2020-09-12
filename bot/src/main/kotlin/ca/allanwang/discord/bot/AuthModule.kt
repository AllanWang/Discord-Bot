package ca.allanwang.discord.bot

import ca.allanwang.discord.bot.core.BotFeature
import ca.allanwang.discord.bot.core.CoreModule
import ca.allanwang.discord.bot.core.PrivProperties
import com.google.common.flogger.FluentLogger
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import java.util.*
import javax.inject.Named
import javax.inject.Singleton

@Module(includes = [CoreModule::class])
object AuthModule {

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
        @PrivProperties privProperties: Properties
    ): String =
        args.firstOrNull()?.also {
            logger.atInfo().log("Got token from args")
        } ?: privProperties.getProperty(kordTokenPropKey)?.also {
            logger.atInfo().log("Got token from properties")
        } ?: error("missing bot token")

}

@Singleton
@Component(modules = [AuthModule::class])
interface AuthComponent {

    @Named("kordToken")
    fun token(): String

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun args(args: Array<String>): Builder

        fun build(): AuthComponent
    }
}