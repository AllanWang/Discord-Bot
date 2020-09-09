package ca.allanwang.discord.bot.game

import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.behavior.channel.createEmbed
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.on
import com.google.common.flogger.FluentLogger

suspend fun Kord.gameBot() {
    val logger = FluentLogger.forEnclosingClass()

    logger.atInfo().log("Load")

    on<MessageCreateEvent> {
        if (message.author?.isBot == true) return@on
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