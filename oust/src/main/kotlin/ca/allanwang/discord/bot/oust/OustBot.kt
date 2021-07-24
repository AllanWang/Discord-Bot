package ca.allanwang.discord.bot.oust

import ca.allanwang.discord.bot.base.*
import ca.allanwang.discord.bot.oust.game.*
import com.google.common.flogger.FluentLogger
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent
import dev.kord.common.Color
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.MessageChannelBehavior
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class OustBot @Inject constructor(
    private val kord: Kord,
    colorPalette: ColorPalette,
    private val oustProvider: Provider<OustComponent.Builder>
) : CommandHandlerBot {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    override val embedColor: Color = colorPalette.orange

    override val handler = commandBuilder(
        "oust",
        CommandHandler.Type.Prefix,
    ) {
        hiddenHelp = true
        action(withMessage = false) {
            test()
        }
    }

    private suspend fun CommandHandlerEvent.test() {
        logger.atInfo().log("Oust test")
        val game = OustGame.create(
            (0..2).map {
                OustPlayer.Info(
                    id = event.message.author!!.id.asString,
                    name = buildString {
                        append(event.message.author!!.username)
                        append(' ')
                        append(it)
                    }
                )
            }
        )
        val component = oustProvider.get()
            .channel(channel)
            .game(game)
            .build()

        withTimeout(TimeUnit.HOURS.toMillis(6)) {
            component.controller().launch()
        }
    }
}

@Module(subcomponents = [OustComponent::class])
interface OustModule

@OustScope
@Subcomponent(modules = [OustGameModule::class])
interface OustComponent {

    @OustScope
    fun controller(): OustController

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun channel(channel: MessageChannelBehavior): Builder

        @BindsInstance
        fun game(game: OustGame): Builder

        fun build(): OustComponent
    }
}

@Module(includes = [OustTurnModule::class])
interface OustGameModule
