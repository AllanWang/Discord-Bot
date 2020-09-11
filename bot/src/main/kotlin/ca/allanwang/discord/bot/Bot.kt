@file:JvmName("Bot")

package ca.allanwang.discord.bot

import ca.allanwang.discord.bot.core.BotFeature
import ca.allanwang.discord.bot.echo.EchoBotModule
import ca.allanwang.discord.bot.time.TimeBotModule
import com.gitlab.kordlib.core.Kord
import com.google.common.flogger.FluentLogger
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton

suspend fun main(args: Array<String>) {

    val logger = FluentLogger.forEnclosingClass()

    logger.atInfo().log("Initialized Bot")

    val component = DaggerBotComponent.builder().args(args).build()

    val kord = Kord(component.token())

    val allFeatures = component.features()

    fun Collection<BotFeature>.logText() = map { it::class.simpleName }

    logger.atInfo().log("Initializing %s", allFeatures.logText())

    val (valid, invalid) = component.features().partition {
        it.init()
    }

    if (invalid.isNotEmpty())
        logger.atSevere().log("Invalid features: %s", invalid.logText())

    logger.atInfo().log("Attaching %s", valid.logText())

    valid.forEach {
        with(it) { kord.attach() }
    }

    logger.atInfo().log(
        """
        
        
        ----------------------------------------
                      Ready
        ----------------------------------------
        
        
    """.trimIndent()
    )

    kord.login { playing("beep boop") }
}

@Singleton
@Component(
    modules = [
        BotModule::class,
        TimeBotModule::class,
        EchoBotModule::class
    ]
)
interface BotComponent {

    @Named("kordToken")
    fun token(): String

    fun features(): Set<@JvmSuppressWildcards BotFeature>

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun args(args: Array<String>): Builder

        fun build(): BotComponent
    }
}
