@file:JvmName("Bot")

package ca.allanwang.discord.bot

import ca.allanwang.discord.bot.base.AdminBotModule
import ca.allanwang.discord.bot.base.CommandBotModule
import ca.allanwang.discord.bot.base.PrefixBotModule
import ca.allanwang.discord.bot.base.StartBotModule
import ca.allanwang.discord.bot.core.BotFeature
import ca.allanwang.discord.bot.oust.OustBotModule
import ca.allanwang.discord.bot.random.RandomBotModule
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

    fun Collection<Any>.logText() = map { it::class.simpleName }

    logger.atInfo().log("Initializing %s", allFeatures.logText())

    val (valid, invalid) = botComponent.features().partition {
        it.init()
    }

    if (invalid.isNotEmpty())
        logger.atSevere().log("Invalid features: %s", invalid.logText())

    logger.atInfo().log("Attaching %s", valid.logText())

//    kord.getApplicationInfo().owner.getDmChannel().createEmbed {
//        title = "Start Report"
//        description = "Test"
//    }

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
        StartBotModule::class,
        CommandBotModule::class,
        AdminBotModule::class,
        PrefixBotModule::class,
        RandomBotModule::class,
        TimeBotModule::class,
        OustBotModule::class,

        // ca.allanwang.discord.bot.base.LogBotModule::class,
        // ca.allanwang.discord.bot.echo.EchoBotModule::class,
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
