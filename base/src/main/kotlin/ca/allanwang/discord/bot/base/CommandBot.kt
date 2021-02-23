package ca.allanwang.discord.bot.base

import ca.allanwang.discord.bot.core.BotFeature
import com.google.common.flogger.FluentLogger
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import java.util.*
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

    data class PrefixedMessage(val prefix: String, val message: String)

    abstract suspend fun MessageCreateEvent.prefixedMessage(): PrefixedMessage?

    final override suspend fun Kord.attach() {
        logger.atInfo().log("Loaded $type handlers %s", handlers.mapNotNull { it::class.simpleName }.sorted())
        val candidates = handlers.onEach { with(it) { attach() } }.candidates()
        val duplicateKeys = handlers.duplicateKeys()
        if (duplicateKeys.isNotEmpty()) {
            logger.atWarning().log("Duplicate commands found: %s", duplicateKeys)
        }
        onMessage {
            handleCommands(candidates)
        }
    }

    private suspend fun MessageCreateEvent.handleCommands(candidates: Map<String, CommandHandler>) {
        val prefixedMessage = prefixedMessage() ?: return
        val key = prefixedMessage.message.substringBefore(' ')
        val handler = candidates[key.toLowerCase(Locale.US)] ?: return
        kord.launch(
            CoroutineExceptionHandler { _, throwable ->
                logger.atWarning().withCause(throwable).log("Failure for %s", handler::class.simpleName)
            }
        ) {
            handler.handle(
                CommandHandlerEvent(
                    event = this@handleCommands,
                    prefix = prefixedMessage.prefix,
                    command = key,
                    message = prefixedMessage.message,
                    origMessage = message.content,
                    commandHelp = handler
                )
            )
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

    override suspend fun MessageCreateEvent.prefixedMessage(): PrefixedMessage? {
        val prefix = prefixSupplier.prefix(groupSnowflake())
        if (!message.content.startsWith(prefix)) return null
        logger.atFine().log("Prefix matched")
        return PrefixedMessage(prefix = prefix, message = message.content.substringAfter(prefix))
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
    private val mentionRegex = Regex("^(<@!?${kord.selfId.asString}>) (.*)$")

    override suspend fun MessageCreateEvent.prefixedMessage(): PrefixedMessage? {
        val match = mentionRegex.find(message.content) ?: return null
        logger.atInfo().log("Bot mention matched")
        val prefix = match.groupValues[1]
        val message = match.groupValues[2].takeIf { it.isNotBlank() } ?: return null
        return PrefixedMessage(prefix = prefix, message = message)
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
