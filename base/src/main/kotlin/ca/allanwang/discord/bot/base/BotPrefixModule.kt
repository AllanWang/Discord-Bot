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

/**
 * Prefix feature bot.
 *
 * This bot is in charge of all [CommandHandlerBot]s with [CommandHandler.Type.Prefix].
 *
 * Each message is checked against the prefix, before being fed to the handler with a matching key (first word)
 */
@Singleton
class BotPrefixGroupFeature @Inject constructor(
    private val prefixSupplier: BotPrefixSupplier,
    handlers: Set<@JvmSuppressWildcards CommandHandlerBot>
) : BotFeature {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    private val handlers = handlers.withType(CommandHandler.Type.Prefix)

    override suspend fun Kord.attach() {
        logger.atInfo().log("Loaded prefix handler %s", handlers.map { it::class.simpleName })
        val candidates = handlers.candidates()
        val duplicateKeys = handlers.duplicateKeys()
        if (duplicateKeys.isNotEmpty()) {
            logger.atWarning().log("Duplicate commands found: %s", duplicateKeys)
        }
        on<MessageCreateEvent> {
            if (message.author?.isBot == true) return@on
            val prefix = prefixSupplier.prefix()
            if (!message.content.startsWith(prefix)) return@on
            logger.atFine().log("Prefix matched")
            val actualMessage = message.content.substringAfter(prefix)
            val key = actualMessage.substringBefore(' ')
            val handler = candidates[key] ?: return@on
            runCatching {
                handler.handle(this, actualMessage)
            }.onFailure {
                logger.atWarning().withCause(it).log("Failure for %s", handler::class.simpleName)
            }
        }
    }
}

@Module(includes = [BotPrefixModule::class])
object PrefixBotFeatureModule {
    @Provides
    @IntoSet
    @Singleton
    fun prefixBot(bot: BotPrefixGroupFeature): BotFeature = bot
}