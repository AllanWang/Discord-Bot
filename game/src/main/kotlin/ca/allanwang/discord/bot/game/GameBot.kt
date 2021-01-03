package ca.allanwang.discord.bot.game

import ca.allanwang.discord.bot.base.onMessage
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.event.message.MessageCreateEvent
import com.google.common.flogger.FluentLogger

suspend fun Kord.gameBot() {
    val logger = FluentLogger.forEnclosingClass()

    logger.atInfo().log("Load")

    onMessage {
        if (message.content == "!game") createIntroMessage()
    }
}

private suspend fun MessageCreateEvent.createIntroMessage() {
    message.channel.createEmbed {
        title = "Test title"
        field {
            inline = true
            name = "Test field 1"
            value = "Inline field"
        }
        field {
            inline = false
            name = "Test field 1"
            value = "Inline field"
        }
    }
}