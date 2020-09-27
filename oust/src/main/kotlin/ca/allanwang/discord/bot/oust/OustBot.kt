package ca.allanwang.discord.bot.oust

import ca.allanwang.discord.bot.base.CommandHandler
import ca.allanwang.discord.bot.base.CommandHandlerBot
import ca.allanwang.discord.bot.base.CommandHandlerEvent
import ca.allanwang.discord.bot.base.commandBuilder
import ca.allanwang.discord.bot.oust.game.*
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.behavior.channel.MessageChannelBehavior
import com.google.common.flogger.FluentLogger
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class OustBot @Inject constructor(
    private val kord: Kord,
    private val oustProvider: Provider<OustComponent.Builder>
) : CommandHandlerBot {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    override val handler = commandBuilder(CommandHandler.Type.Prefix) {
        arg("oust") {
            action(withMessage = false) {
                test()
            }
        }
    }

    private suspend fun CommandHandlerEvent.test() {
        logger.atInfo().log("Oust test")
        val game = OustGame.create(
            (0..2).map {
                OustPlayer.Info(
                    id = event.message.author!!.id.value,
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

        kord.launch {
            withTimeout(TimeUnit.HOURS.toMillis(6)) {
                component.controller().launch()
            }
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
