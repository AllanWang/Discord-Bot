package ca.allanwang.discord.bot.base

import com.gitlab.kordlib.core.event.message.MessageCreateEvent

@DslMarker
@Target(AnnotationTarget.CLASS)
annotation class BotCommandDsl

@BotCommandDsl
interface CommandBuilderActionDsl {
    var help: String?

    var action: CommandHandler
}

@BotCommandDsl
interface CommandBuilderArgDsl {

    var help: String?

    fun arg(arg: String, help: String? = null, block: CommandBuilderArgDsl.() -> Unit)

    fun action(help: String? = null, action: suspend (event: MessageCreateEvent, message: String) -> Unit)

}

interface CommandHandler {
    suspend fun handle(event: MessageCreateEvent, message: String)
}

fun commandBuilder(arg: String, block: CommandBuilderArgDsl.() -> Unit): CommandHandler =
    CommandBuilderRoot(arg = arg).apply(block).also { it.finish() }

internal class CommandBuilderRoot(arg: String) : CommandBuilderArg("", arg = arg) {

    override suspend fun handle(event: MessageCreateEvent, message: String) {
        if (!message.startsWith(arg)) return
        super.handle(event, message.substringAfter(arg))
    }
}

internal open class CommandBuilderArg(val prefix: String, val arg: String) : CommandBuilderArgDsl, CommandHandler {

    private var children: MutableMap<String, CommandBuilderArg> = mutableMapOf()

    private var action: CommandBuilderAction? = null

    override var help: String? = null

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

    override suspend fun handle(event: MessageCreateEvent, message: String) {
        val key = message.substringBefore(' ')
        val argHandler = children[key]
        if (argHandler != null) {
            argHandler.handle(event, message.substringAfter(' '))
            return
        }
        action?.handle(event, message)
    }

    override fun arg(arg: String, help: String?, block: CommandBuilderArgDsl.() -> Unit) {
        val builder = CommandBuilderArg(command, arg).apply {
            this.help = help
            block()
            finish()
        }
        children[builder.arg] = builder
    }

    override fun action(help: String?, action: suspend (event: MessageCreateEvent, message: String) -> Unit) {
        val builder = CommandBuilderAction().apply {
            this.help = help
            this.action = object : CommandHandler {
                override suspend fun handle(event: MessageCreateEvent, message: String) {
                    action(event, message)
                }
            }
            finish()
        }
        this.action = builder
    }

    internal fun finish() {
        children.toSortedMap()
    }
}

internal class CommandBuilderAction : CommandBuilderActionDsl, CommandHandler {

    override var help: String? = null

    override var action: CommandHandler = object : CommandHandler {
        override suspend fun handle(event: MessageCreateEvent, message: String) {

        }
    }

    override suspend fun handle(event: MessageCreateEvent, message: String) {
        action.handle(event, message)
    }

    internal fun finish() {

    }
}