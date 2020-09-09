package ca.allanwang.discord.bot.echo

import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.on
import com.google.common.flogger.FluentLogger
import org.slf4j.LoggerFactory

suspend fun Kord.echoBot() {
    val logger = FluentLogger.forEnclosingClass()

    logger.atInfo().log("Load")

    on<MessageCreateEvent> {
        if (message.author?.isBot == true) return@on
        if (message.content == "!ping") message.channel.createMessage("pong")
    }
}