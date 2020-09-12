@file:JvmName("Bot")

package ca.allanwang.discord.bot

import ca.allanwang.discord.bot.base.PrefixBotFeatureModule
import ca.allanwang.discord.bot.core.BotFeature
import ca.allanwang.discord.bot.echo.EchoBotModule
import ca.allanwang.discord.bot.time.TimeBotModule
import com.gitlab.kordlib.core.Kord
import com.google.common.flogger.FluentLogger
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

suspend fun main(args: Array<String>) {

    val logger = FluentLogger.forEnclosingClass()

    logger.atInfo().log("Initialized Bot")

    val authComponent = DaggerAuthComponent.builder().args(args).build()

    val kord = Kord(authComponent.token())

    val botComponent = DaggerBotComponent.builder().kord(kord).build()

    val allFeatures = botComponent.features()

    fun Collection<BotFeature>.logText() = map { it::class.simpleName }

    logger.atInfo().log("Initializing %s", allFeatures.logText())

    val (valid, invalid) = botComponent.features().partition {
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
        PrefixBotFeatureModule::class,
        TimeBotModule::class,
        EchoBotModule::class
    ]
)
interface BotComponent {

    fun features(): Set<@JvmSuppressWildcards BotFeature>

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun kord(kord: Kord): Builder

        fun build(): BotComponent
    }
}
