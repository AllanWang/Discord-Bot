package ca.allanwang.discord.bot.base

import ca.allanwang.discord.bot.firebase.FirebaseRootRef
import ca.allanwang.discord.bot.firebase.listenSnapshot
import ca.allanwang.discord.bot.firebase.setValue
import com.google.common.flogger.FluentLogger
import com.google.firebase.database.DatabaseReference
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
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

    private val ref: DatabaseReference = rootRef.child(PREFIX)

    suspend fun setPrefix(group: Snowflake, prefix: String): Boolean = ref.child(group.asString).setValue(prefix)

    suspend fun listen(): Flow<Map<Snowflake, String>> = ref.listenSnapshot().filterNotNull().map { snapshot ->
        snapshot.children
            .mapNotNull {
                runCatching {
                    Snowflake(it.key) to it.getValue(String::class.java)
                }.onFailure {
                    logger.atWarning().withCause(it).log("Prefix decode failure at ${snapshot.key}")
                }.getOrNull()
            }.toMap()
    }
}

@Singleton
class PrefixBot @Inject constructor(
    private val prefixApi: PrefixApi,
    private val botPrefixSupplier: BotPrefixSupplier,
) : CommandHandlerBot {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
        private val whitespaceRegex = Regex("\\s")
    }

    override val handler = commandBuilder(CommandHandler.Type.Prefix, CommandHandler.Type.Mention) {
        arg("prefix") {
            action(withMessage = true) {
                prefix()
            }
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
