package ca.allanwang.discord.bot.oust

import ca.allanwang.discord.bot.base.CommandHandler
import ca.allanwang.discord.bot.base.CommandHandlerBot
import ca.allanwang.discord.bot.base.CommandHandlerEvent
import ca.allanwang.discord.bot.base.commandBuilder
import ca.allanwang.discord.bot.oust.game.OustClient
import ca.allanwang.discord.bot.oust.game.OustController
import ca.allanwang.discord.bot.oust.game.OustGame
import ca.allanwang.discord.bot.oust.game.OustPlayer
import com.gitlab.kordlib.core.Kord
import com.google.common.flogger.FluentLogger
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
                    id = event.message.author!!.id,
                    name = event.message.author!!.username
                )
            }.take(3).toList()
        )
        val controller = OustController(
            game = game,
            client = OustClient(kord = kord, channel = event.message.channel)
        )
        controller.test()
    }

}
