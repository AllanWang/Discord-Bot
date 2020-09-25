package ca.allanwang.discord.bot.oust

import ca.allanwang.discord.bot.base.CommandHandler
import ca.allanwang.discord.bot.base.CommandHandlerBot
import ca.allanwang.discord.bot.base.CommandHandlerEvent
import ca.allanwang.discord.bot.base.commandBuilder
import ca.allanwang.discord.bot.oust.game.*
import com.gitlab.kordlib.core.Kord
import com.google.common.flogger.FluentLogger
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OustBot @Inject constructor(
    private val kord: Kord
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
            generateSequence {
                OustPlayer.Info(
                    id = event.message.author!!.id.value,
                    name = event.message.author!!.username
                )
            }.take(3).toList()
        )
        val component = DaggerOustComponent.builder().game(game).build()
        component.controller().test()
    }

}

@OustScope
@Component(modules = [OustGameModule::class])
interface OustComponent {

    @OustScope
    fun controller(): OustController

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun game(game: OustGame): Builder

        fun build(): OustComponent
    }
}

@Module(includes = [OustTurnModule::class])
interface OustGameModule
