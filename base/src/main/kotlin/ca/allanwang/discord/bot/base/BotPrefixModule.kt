package ca.allanwang.discord.bot.base

import ca.allanwang.discord.bot.core.BotFeature
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.core.on
import com.google.common.flogger.FluentLogger
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BotPrefixGroupFeature @Inject constructor(
    private val prefixSupplier: BotPrefixSupplier,
    private val handlers: Set<@JvmSuppressWildcards CommandHandler>
) : BotFeature {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    override suspend fun Kord.attach() {
        logger.atInfo().log("Loaded handler %s", handlers.map { it::class.simpleName })
        on<MessageCreateEvent> {
            if (message.author?.isBot == true) return@on
            val prefix = prefixSupplier.prefix()
            if (!message.content.startsWith(prefix)) return@on
            val actualMessage = message.content.substringAfter(prefix)
            logger.atInfo().log("Prefix matched")
            handlers.forEach { handler ->
                runCatching {
                    handler.handle(this, actualMessage)
                }.onFailure {
                    logger.atWarning().withCause(it).log("Failure for %s", handler::class.simpleName)
                }
            }
            if (message.content == "!ping") message.channel.createMessage("pong")
        }
    }
}

@Module(includes = [BotPrefixModule::class])
object PrefixBot {
    @Provides
    @IntoSet
    @Singleton
    fun prefixBot(bot: BotPrefixGroupFeature): BotFeature = bot
}