package ca.allanwang.discord.bot.base

import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.rest.builder.message.EmbedBuilder
import com.google.common.flogger.FluentLogger

@DslMarker
@Target(AnnotationTarget.CLASS)
annotation class BotCommandDsl

@BotCommandDsl
interface CommandBuilderRootDsl {

    var help: String?

    fun arg(arg: String, help: String? = null, block: CommandBuilderArgDsl.() -> Unit)

}

@BotCommandDsl
interface CommandBuilderArgDsl : CommandBuilderRootDsl {

    fun action(
        withMessage: Boolean,
        help: String? = null,
        action: CommandHandlerAction
    )

}

@BotCommandDsl
interface CommandBuilderActionDsl {
    var help: String?

    var withMessage: Boolean

    var action: CommandHandlerAction
}

fun commandBuilder(type: CommandHandler.Type, block: CommandBuilderRootDsl.() -> Unit): CommandHandler =
    CommandBuilderRoot(type).apply(block).also { it.finish() }

internal open class CommandBuilderBase : CommandBuilderRootDsl {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    protected var children: MutableMap<String, CommandBuilderArg> = mutableMapOf()

    val keys: Set<String> get() = children.keys

    suspend fun handle(event: MessageCreateEvent, message: String) {
        handleImpl(event, message)
    }

    override var help: String? = null

    protected open suspend fun handleImpl(event: MessageCreateEvent, message: String): Boolean {
        val key = message.substringBefore(' ')
        logger.atFine().log("Test key %s in %s", key, keys)
        val argHandler = children[key]
        val subMessage = if (key == message) "" else message.substringAfter(' ')
        if (argHandler != null) {
            argHandler.handle(event, subMessage)
            return true
        }
        return false
    }

    override fun arg(arg: String, help: String?, block: CommandBuilderArgDsl.() -> Unit) {
        val builder = CommandBuilderArg("", arg).apply {
            this.help = help
            block()
            finish()
        }
        children[builder.arg] = builder
    }

    internal fun finish() {
        children = children.toSortedMap()
    }
}

internal class CommandBuilderRoot(override val type: CommandHandler.Type) : CommandBuilderBase(), CommandBuilderRootDsl,
    CommandHandler

internal class CommandBuilderArg(
    val prefix: String, val arg: String
) : CommandBuilderBase(), CommandBuilderArgDsl {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    private var action: CommandBuilderAction? = null

    private val command: String = if (prefix.isBlank()) arg else "$prefix $arg"

//    suspend fun EmbedBuilder.createHelp() {
//        title = command
//        description = help
//        children.forEach {
//            field {
//                name = it.arg.takeIf { it.isNotEmpty() } ?: EmbedBuilder.ZERO_WIDTH_SPACE
//                value = it.help ?: ""
//            }
//        }
//    }

    override suspend fun handleImpl(event: MessageCreateEvent, message: String): Boolean {
        if (super.handleImpl(event, message)) return true
        val action = action ?: return false
        val actionEvent = CommandHandlerEvent(
            command = command,
            event = event,
            message = message
        )
        if (action.withMessage) action.action(actionEvent)
        else if (message.isBlank()) action.action(actionEvent)
        return true
    }

    override fun arg(arg: String, help: String?, block: CommandBuilderArgDsl.() -> Unit) {
        val builder = CommandBuilderArg(command, arg).apply {
            this.help = help
            block()
            finish()
        }
        children[builder.arg] = builder
    }

    override fun action(
        withMessage: Boolean,
        help: String?,
        action: CommandHandlerAction
    ) {
        val builder = CommandBuilderAction().apply {
            this.withMessage = withMessage
            this.help = help
            this.action = action
            finish()
        }
        this.action = builder
    }
}

internal class CommandBuilderAction : CommandBuilderActionDsl {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()

        private val HANDLER_NOOP: CommandHandlerAction = {
            logger.atWarning().log("NOOP Called")
        }
    }

    override var help: String? = null

    override var withMessage: Boolean = false

    override var action: CommandHandlerAction = HANDLER_NOOP

    internal fun finish() {

    }
}