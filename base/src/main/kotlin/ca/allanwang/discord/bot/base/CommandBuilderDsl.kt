package ca.allanwang.discord.bot.base

import com.google.common.flogger.FluentLogger
import dev.kord.core.behavior.channel.createEmbed
import java.util.*

@DslMarker
@Target(AnnotationTarget.CLASS)
annotation class BotCommandDsl

data class HelpContext(
    val prefix: String
)

@BotCommandDsl
interface CommandBuilderRootDsl {
    fun arg(arg: String, block: CommandBuilderArgDsl.() -> Unit)
}

@BotCommandDsl
interface CommandBuilderArgDsl : CommandBuilderRootDsl {

    var autoGenHelp: Boolean

    fun action(withMessage: Boolean, action: CommandHandlerAction)
}

@BotCommandDsl
interface CommandBuilderActionDsl {
    var withMessage: Boolean

    var action: CommandHandlerAction

    fun help(action: HelpSupplier)
}

typealias HelpSupplier = HelpContext.() -> String

fun CommandHandlerBot.commandBuilder(
    vararg types: CommandHandler.Type,
    block: CommandBuilderRootDsl.() -> Unit
): CommandHandler =
    CommandBuilderRoot(types.toSet()).apply(block).also { it.finish() }

internal open class CommandBuilderBase : CommandBuilderRootDsl {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    protected var children: MutableMap<String, CommandBuilderArg> = mutableMapOf()

    val keys: Set<String> get() = children.keys

    suspend fun handle(event: CommandHandlerEvent) {
        handleImpl(event)
    }

    protected open suspend fun handleImpl(event: CommandHandlerEvent): Boolean {
        val key = event.message.substringBefore(' ')
        logger.atFine().log("Test key %s in %s", key, keys)
        val argHandler = children[key.toLowerCase(Locale.US)]
        val subMessage = if (key == event.message) "" else event.message.substringAfter(' ')
        if (argHandler != null) {
            argHandler.handle(event.copy(message = subMessage))
            return true
        }
        return false
    }

    override fun arg(arg: String, block: CommandBuilderArgDsl.() -> Unit) {
        val builder = CommandBuilderArg("", arg).apply {
            block()
            finish()
        }
        children[builder.arg.toLowerCase(Locale.US)] = builder
    }

    internal fun finish() {
        children = children.toSortedMap()
    }
}

internal class CommandBuilderRoot(override val types: Set<CommandHandler.Type>) :
    CommandBuilderBase(),
    CommandBuilderRootDsl,
    CommandHandler

internal class CommandBuilderArg(
    val prevCommand: String,
    val arg: String
) : CommandBuilderBase(), CommandBuilderArgDsl {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    override var autoGenHelp: Boolean = true

    private var action: CommandBuilderAction? = null

    private var help: HelpSupplier? = null

    private val command: String = if (prevCommand.isBlank()) arg else "$prevCommand $arg"

    override suspend fun handleImpl(event: CommandHandlerEvent): Boolean {
        if (super.handleImpl(event)) return true
        if (handleHelp(event)) return true
        if (handleAction(event)) return true
        return false
    }

    private suspend fun handleAction(event: CommandHandlerEvent): Boolean {
        val action = action ?: return false
        val actionEvent = event.copy(command = command)
        if (action.withMessage) action.action(actionEvent)
        else if (event.message.isBlank()) action.action(actionEvent)
        return true
    }

    private suspend fun handleHelp(event: CommandHandlerEvent): Boolean {
        if (!autoGenHelp) return false
        val key = event.message.substringBefore(' ').toLowerCase(Locale.US)
        if (key != "help") return false
        val context = HelpContext(prefix = event.prefix)
        event.channel.createEmbed {
            title = command
            description = help?.invoke(context)
            val childHelp = childHelp(context)
            if (childHelp != null) {
                field {
                    name = "Commands"
                    value = childHelp
                }
            }
        }
        return true
    }

    override fun arg(arg: String, block: CommandBuilderArgDsl.() -> Unit) {
        val builder = CommandBuilderArg(command, arg).apply {
            block()
            finish()
        }
        children[builder.arg.toLowerCase(Locale.US)] = builder
    }

    override fun action(withMessage: Boolean, action: CommandHandlerAction) {
        val builder = CommandBuilderAction().apply {
            this.withMessage = withMessage
            this.action = action
            finish()
        }
        this.action = builder
    }

    private fun childHelp(context: HelpContext): String? {
        val nestedHelp = children.values.map { it.help(context) }
        if (nestedHelp.isEmpty()) return null
        return buildString {
            nestedHelp.forEach {
                append(it)
                append("\n\n")
            }
        }
    }

    fun help(context: HelpContext): String {
        val currentHelp = help?.invoke(context)
        return buildString {
            appendCodeBlock {
                append(context.prefix)
                append(command)
            }
            /*
             * TODO
             *
             * Remove help from arg? Only add to root and action.
             * Avoid double line breaks.
             * Maybe use dsl to add actions to avoid duplicate parsing language
             * Support help markdown mode?
             */
            if (currentHelp != null) {
                append(": ")
                append(currentHelp)
            }
            append("\n\n")
            append(childHelp(context))
        }
    }
}

internal class CommandBuilderAction : CommandBuilderActionDsl {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()

        private val HANDLER_NOOP: CommandHandlerAction = {
            logger.atWarning().log("NOOP Called")
        }
    }

    var hasHelp: Boolean = false

    var help: HelpSupplier? = null

    override var withMessage: Boolean = false

    override var action: CommandHandlerAction = HANDLER_NOOP

    internal fun finish() {
    }

    override fun help(action: HelpSupplier) {
        hasHelp = true
        help = action
    }
}
