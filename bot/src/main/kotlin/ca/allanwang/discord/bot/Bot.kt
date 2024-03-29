@file:JvmName("Bot")

package ca.allanwang.discord.bot

import ca.allanwang.discord.bot.base.*
import ca.allanwang.discord.bot.cinco.CincoBotModule
import ca.allanwang.discord.bot.core.BotFeature
import ca.allanwang.discord.bot.echo.EchoBotModule
import ca.allanwang.discord.bot.gameevent.GameEventBotModule
import ca.allanwang.discord.bot.oust.OustBotModule
import ca.allanwang.discord.bot.overwatch.OverwatchBotModule
import ca.allanwang.discord.bot.qotd.QotdBotModule
import ca.allanwang.discord.bot.random.RandomBotModule
import ca.allanwang.discord.bot.time.TimeBotModule
import com.google.common.flogger.FluentLogger
import dagger.BindsInstance
import dagger.Component
import dev.kord.core.Kord
import kotlinx.datetime.Clock
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

    kord.login {
        presence {
            playing("beep boop")
            since = Clock.System.now()
        }
    }
}

@Singleton
@Component(
    modules = [
        StartBotModule::class,
        EchoBotModule::class,
        CommandBotModule::class,
        AdminBotModule::class,
        DevBotModule::class,
        PrefixBotModule::class,
        HelpBotModule::class,
        RandomBotModule::class,
        TimeBotModule::class,
        OustBotModule::class,
        CincoBotModule::class,
        OverwatchBotModule::class,
        QotdBotModule::class,
        GameEventBotModule::class,

        // ca.allanwang.discord.bot.base.LogBotModule::class,
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
