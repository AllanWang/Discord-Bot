package ca.allanwang.discord.bot.base

import ca.allanwang.discord.bot.gradle.Build
import com.gitlab.kordlib.core.behavior.channel.createEmbed
import com.google.common.flogger.FluentLogger
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import java.awt.Color
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminBot @Inject constructor(
) : CommandHandlerBot {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()

        private val embedColor: Color = Color.decode("#4DB6C1")
    }

    override val handler = commandBuilder(CommandHandler.Type.Mention) {
        arg("info") {
            action(withMessage = false) {
                buildInfo()
            }
        }
    }

    private suspend fun CommandHandlerEvent.buildInfo() {
        if (!Build.valid) {
            channel.createMessage("Invalid build info; source not built from git repo")
            return
        }
        channel.createEmbed {
            color = embedColor
            title = "Build Info"
            field {
                name = "Version"
                value = buildString { appendLink(Build.version, Build.commitUrl) }
            }
            field {
                name = "Build Time"
                value = Build.buildTime
            }
        }
    }
}

@Module(includes = [BotPrefixModule::class])
object AdminBotModule {

    @Provides
    @IntoSet
    @Singleton
    fun bot(bot: AdminBot): CommandHandlerBot = bot
}