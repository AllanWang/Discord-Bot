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

abstract class CommandBot(
    handlers: Set<@JvmSuppressWildcards CommandHandlerBot>,
    private val type: CommandHandler.Type
) : BotFeature {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    private val handlers = handlers.withType(type)

    abstract suspend fun MessageCreateEvent.actualMessage(): String?

    final override suspend fun Kord.attach() {
        logger.atInfo().log("Loaded $type handlers %s", handlers.map { it::class.simpleName })
        val candidates = handlers.candidates()
        val duplicateKeys = handlers.duplicateKeys()
        if (duplicateKeys.isNotEmpty()) {
            logger.atWarning().log("Duplicate commands found: %s", duplicateKeys)
        }
        on<MessageCreateEvent> {
            if (message.author?.isBot == true) return@on
            val actualMessage = actualMessage() ?: return@on
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
) : CommandBot(handlers = handlers, type = CommandHandler.Type.Prefix) {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    override suspend fun MessageCreateEvent.actualMessage(): String? {
        val prefix = prefixSupplier.prefix(this)
        if (!message.content.startsWith(prefix)) return null
        logger.atFine().log("Prefix matched")
        return message.content.substringAfter(prefix)
    }
}

@Singleton
class BotMentionGroupFeature @Inject constructor(
    handlers: Set<@JvmSuppressWildcards CommandHandlerBot>,
    kord: Kord
) : CommandBot(handlers = handlers, type = CommandHandler.Type.Mention) {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    /**
     * Discord ids are sent via text via `<@{id}>`.
     * If there is a nickname, an additional `!` will follow `@`
     */
    private val mentionRegex = Regex("^<@!?${kord.selfId.value}> (.*)$")

    override suspend fun MessageCreateEvent.actualMessage(): String? {
        val match = mentionRegex.find(message.content) ?: return null
        logger.atInfo().log("Bot mention matched")
        return match.groupValues.getOrNull(1)
    }
}

@Module(includes = [BotPrefixModule::class])
object CommandBotModule {
    @Provides
    @IntoSet
    @Singleton
    fun prefixBot(bot: BotPrefixGroupFeature): BotFeature = bot

    @Provides
    @IntoSet
    @Singleton
    fun mentionBot(bot: BotMentionGroupFeature): BotFeature = bot
}