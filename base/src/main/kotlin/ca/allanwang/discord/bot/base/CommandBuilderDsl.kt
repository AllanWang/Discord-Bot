package ca.allanwang.discord.bot.base

import com.google.common.flogger.FluentLogger
import dev.kord.common.Color
import java.util.*

@DslMarker
@Target(AnnotationTarget.CLASS)
annotation class BotCommandDsl

data class HelpContext(
    val prefix: String
)

@BotCommandDsl
interface CommandBuilderBaseDsl : CommandHelp {
    var autoGenHelp: Boolean

    fun arg(arg: String, block: CommandBuilderArgDsl.() -> Unit)
}

@BotCommandDsl
interface CommandBuilderRootDsl : CommandBuilderBaseDsl {

    val color: Color

    var description: String?
}

@BotCommandDsl
interface CommandBuilderArgDsl : CommandBuilderBaseDsl {

    fun action(withMessage: Boolean, helpArgs: String? = null, help: HelpSupplier? = null, action: CommandHandlerAction)
}

@BotCommandDsl
interface CommandBuilderActionDsl {
    var withMessage: Boolean

    var action: CommandHandlerAction
}

typealias HelpSupplier = HelpContext.() -> String

fun CommandHandlerBot.commandBuilder(
    vararg types: CommandHandler.Type,
    color: Color,
    block: CommandBuilderRootDsl.() -> Unit
): CommandHandler = CommandBuilderRoot(types = types.toSet(), color = color).apply(block)

internal abstract class CommandBuilderBase : CommandBuilderBaseDsl {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    override var autoGenHelp: Boolean = true

    /**
     * Command used to reach this node. Blank for roots
     */
    protected abstract val command: String

    protected abstract val root: CommandBuilderRootDsl

    protected val children: MutableMap<String, CommandBuilderArg> = mutableMapOf()

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
            argHandler.handle(event.copy(message = subMessage, commandHelp = argHandler))
            return true
        }
        return false
    }

    override suspend fun handleHelp(event: CommandHandlerEvent) {
        if (!autoGenHelp) return
        val context = HelpContext(prefix = event.prefix)

        val commands =
            help(context).chunkedByLength(length = 1024 /* max field length */, emptyText = "No commands found.")

        event.channel.paginatedMessage(commands) { desc ->
            title = "$command help"
            description = root.description
            color = root.color
            if (desc != null) {
                field {
                    name = "Commands"
                    value = desc
                }
            }
        }
    }

    override fun arg(arg: String, block: CommandBuilderArgDsl.() -> Unit) {
        val builder = CommandBuilderArg(root = root, prevCommand = command, arg = arg).apply {
            block()
            postCreate(this@CommandBuilderBase)
        }
        children[builder.arg.toLowerCase(Locale.US)] = builder
    }
}

internal class CommandBuilderRoot(override val types: Set<CommandHandler.Type>, override val color: Color) :
    CommandBuilderBase(),
    CommandBuilderRootDsl,
    CommandHandler {

    override val root: CommandBuilderRootDsl = this

    override var description: String? = null

    override val command: String = ""

    override fun help(context: HelpContext): List<String> {
        if (!autoGenHelp) return emptyList()
        return children.filterKeys { it != "help" }.values.flatMap { it.help(context) }
    }
}

internal class CommandBuilderArg(
    override val root: CommandBuilderRootDsl,
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
        val actionEvent = event.copy(command = command, commandHelp = this)
        if (action.withMessage) action.action(actionEvent)
        else if (event.message.isBlank()) action.action(actionEvent)
        return true
    }

    override fun action(withMessage: Boolean, helpArgs: String?, help: HelpSupplier?, action: CommandHandlerAction) {
        val builder = CommandBuilderAction(command = command).apply {
            this.withMessage = withMessage
            this.helpArgs = helpArgs
            this.helpSupplier = help
            this.action = action
        }
        this.action = builder
    }

    internal fun postCreate(parent: CommandBuilderBaseDsl) {
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

    override fun help(context: HelpContext): List<String> {
        if (!autoGenHelp) return emptyList()
        val actionHelp = action?.help(context)
        val nestedHelp = children.filterKeys { it != "help" }.values.flatMap { it.help(context) }

        return mutableListOf<String>().apply {
            if (actionHelp != null) add(actionHelp)
            addAll(nestedHelp)
        }
    }
}

internal class CommandBuilderAction(val command: String) : CommandBuilderActionDsl {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()

        private val HANDLER_NOOP: CommandHandlerAction = {
            logger.atWarning().log("NOOP Called")
        }
    }

    internal var helpArgs: String? = null

    internal var helpSupplier: HelpSupplier? = null

    override var withMessage: Boolean = false

    override var action: CommandHandlerAction = HANDLER_NOOP

    fun help(helpContext: HelpContext): String = buildString {
        appendCodeBlock {
            append(helpContext.prefix)
            append(command)
            helpArgs?.let { args ->
                append(" ")
                append(args)
            }
        }
        helpSupplier?.invoke(helpContext)?.let { description ->
            append(": ")
            append(description)
        }
    }
}
