package ca.allanwang.discord.bot.base

import ca.allanwang.discord.bot.firebase.listen
import ca.allanwang.discord.bot.firebase.setValue
import com.gitlab.kordlib.core.Kord
import com.google.common.flogger.FluentLogger
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrefixApi @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
    kord: Kord
) {
    companion object {
        private const val PREFIX = "prefix"
        private val logger = FluentLogger.forEnclosingClass()
    }

    private val selfId = kord.selfId.value

    private val ref: DatabaseReference
        get() = firebaseDatabase.reference.child(selfId).child(PREFIX)

    suspend fun setPrefix(prefix: String): Boolean = ref.setValue(prefix)

    suspend fun listen(): Flow<String> = ref.listen<String>().filterNotNull()
}

@Singleton
class PrefixBot @Inject constructor(
    private val prefixApi: PrefixApi
) : CommandHandlerBot {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
        private val whitespaceRegex = Regex("\\s")
    }

    override val handler = commandBuilder(CommandHandler.Type.Prefix, CommandHandler.Type.Mention) {
        arg("prefix") {
            action(withMessage = true) {
                if (whitespaceRegex.matches(message)) {
                    channel.createMessage("Prefix `$message` cannot contain whitespace")
                    return@action
                }
                logger.atInfo().log("Prefix set to %s", message)
                prefixApi.setPrefix(message)
                channel.createMessage("Set prefix to `$message`")
            }
        }
    }
}

@Module(includes = [BotPrefixModule::class])
object PrefixBotModule {

    @Provides
    @IntoSet
    @Singleton
    fun bot(bot: PrefixBot): CommandHandlerBot = bot
}