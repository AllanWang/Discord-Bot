package ca.allanwang.discord.bot.base

import ca.allanwang.discord.bot.firebase.FirebaseRootRef
import ca.allanwang.discord.bot.firebase.setValue
import ca.allanwang.discord.bot.firebase.singleSnapshot
import com.google.common.flogger.FluentLogger
import com.google.firebase.database.DatabaseReference
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import dev.kord.common.Color
import dev.kord.common.entity.Snowflake
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrefixApi @Inject constructor(
    @FirebaseRootRef rootRef: DatabaseReference
) {
    companion object {
        private const val PREFIX = "prefix"
        private val logger = FluentLogger.forEnclosingClass()
    }

    private val prefixes: ConcurrentHashMap<Snowflake, String> = ConcurrentHashMap()

    val defaultPrefix: String = "!"

    private val ref: DatabaseReference = rootRef.child(PREFIX)

    suspend fun setPrefix(group: Snowflake, prefix: String): Boolean {
        prefixes[group] = prefix
        return ref.child(group.asString).setValue(prefix)
    }

    fun getPrefix(group: Snowflake): String =
        prefixes[group] ?: defaultPrefix

    private suspend fun fetchAll(): Map<Snowflake, String> {
        val snapshot = ref.singleSnapshot()
        return snapshot.children
            .mapNotNull {
                runCatching {
                    Snowflake(it.key) to it.getValue(String::class.java)
                }.onFailure {
                    logger.atWarning().withCause(it).log("Prefix decode failure at ${snapshot.key}")
                }.getOrNull()
            }.toMap()
    }

    suspend fun sync() {
        logger.atInfo().log("Syncing prefixes")
        prefixes.putAll(fetchAll())
    }
}

@Singleton
class PrefixBot @Inject constructor(
    colorPalette: ColorPalette,
    private val prefixApi: PrefixApi,
    private val botPrefixSupplier: BotPrefixSupplier,
) : CommandHandlerBot {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
        private val whitespaceRegex = Regex("\\s")
    }

    override val embedColor: Color = colorPalette.default

    override val handler =
        commandBuilder(
            "prefix",
            CommandHandler.Type.Prefix,
            CommandHandler.Type.Mention,
            description = "Bot prefix configuration"
        ) {
            action(
                withMessage = true, helpArgs = "[prefix]",
                help = {
                    buildString {
                        append("Set new prefix. Default is ")
                        appendCodeBlock { append(prefixApi.defaultPrefix) }
                    }
                }
            ) {
                prefix()
            }
        }

    private suspend fun CommandHandlerEvent.prefix() {
        if (message.isBlank()) {
            channel.createMessage("Prefix is ${botPrefixSupplier.prefix(event.groupSnowflake())}")
            return
        }
        if (whitespaceRegex.containsMatchIn(message)) {
            channel.createMessage("Prefix `$message` cannot contain whitespace")
            return
        }
        logger.atInfo().log("Prefix set to %s", message)
        prefixApi.setPrefix(event.groupSnowflake(), message)
        channel.createMessage("Set prefix to `$message`")
    }
}

@Module(includes = [BotPrefixModule::class])
object PrefixBotModule {

    @Provides
    @IntoSet
    @Singleton
    fun bot(bot: PrefixBot): CommandHandlerBot = bot
}
