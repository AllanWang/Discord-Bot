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
    var autoGenHelp: Boolean

    fun arg(arg: String, block: CommandBuilderArgDsl.() -> Unit)
}

@BotCommandDsl
interface CommandBuilderArgDsl : CommandBuilderRootDsl {

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
): CommandHandler = CommandBuilderRoot(types.toSet()).apply(block)

internal abstract class CommandBuilderBase : CommandBuilderRootDsl {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    override var autoGenHelp: Boolean = true

    /**
     * Command used to reach this node. Blank for roots
     */
    protected abstract val command: String

    protected val children: MutableMap<String, CommandBuilderArg> = sortedMapOf()

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
        val builder = CommandBuilderArg(command, arg).apply {
            block()
            postCreate(this@CommandBuilderBase)
        }
        children[builder.arg.toLowerCase(Locale.US)] = builder
    }
}

internal class CommandBuilderRoot(override val types: Set<CommandHandler.Type>) :
    CommandBuilderBase(),
    CommandBuilderRootDsl,
    CommandHandler {

    override val command: String = ""
}

internal class CommandBuilderArg(
    val prevCommand: String,
    val arg: String
) : CommandBuilderBase(), CommandBuilderArgDsl {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    private var action: CommandBuilderAction? = null

    override val command: String = if (prevCommand.isBlank()) arg else "$prevCommand $arg"

    override suspend fun handleImpl(event: CommandHandlerEvent): Boolean {
        if (super.handleImpl(event)) return true
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

    private suspend fun handleHelp(event: CommandHandlerEvent) {
        if (!autoGenHelp) return
        val context = HelpContext(prefix = event.prefix)
        event.channel.createEmbed {
            title = command
            val helpText = help(context)
            if (helpText != null) {
                field {
                    name = "Commands"
                    value = helpText
                }
            }
        }
    }

    override fun action(withMessage: Boolean, action: CommandHandlerAction) {
        val builder = CommandBuilderAction(command = command).apply {
            this.withMessage = withMessage
            this.action = action
            finish()
        }
        this.action = builder
    }

    internal fun postCreate(parent: CommandBuilderRootDsl) {
        if (!parent.autoGenHelp) {
            autoGenHelp = false
        }
        if (autoGenHelp && !keys.contains("help")) {
            arg("help") {
                autoGenHelp = false
                action(withMessage = false) {
                    this@CommandBuilderArg.handleHelp(this)
                }
            }
        }
    }

    fun help(context: HelpContext): String? {
        /*
         * TODO
         *
         * Remove help from arg? Only add to root and action.
         * Avoid double line breaks.
         * Maybe use dsl to add actions to avoid duplicate parsing language
         * Support help markdown mode?
         */
        val actionHelp = action?.help(context)
        val nestedHelp = children.filterKeys { it != "help" }.values.mapNotNull { it.help(context) }
        if (actionHelp == null && nestedHelp.isEmpty()) return null
        return buildString {
            appendLine(actionHelp)
            nestedHelp.forEach {
                appendLine(it)
            }
        }.trim()
    }
}

internal class CommandBuilderAction(val command: String) : CommandBuilderActionDsl {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()

        private val HANDLER_NOOP: CommandHandlerAction = {
            logger.atWarning().log("NOOP Called")
        }
    }

    private var help: HelpSupplier? = null

    override var withMessage: Boolean = false

    override var action: CommandHandlerAction = HANDLER_NOOP

    internal fun finish() {
    }

    override fun help(action: HelpSupplier) {
        help = action
    }

    fun help(helpContext: HelpContext): String = buildString {
        appendCodeBlock {
            append(helpContext.prefix)
            append(command)
            help?.invoke(helpContext)?.let { description ->
                append(": ")
                append(description)
            }
        }
    }
}
