package ca.allanwang.discord.bot.base

import ca.allanwang.discord.bot.core.Build
import ca.allanwang.discord.bot.core.CoreModule
import com.google.common.flogger.FluentLogger
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import dev.kord.common.Color
import dev.kord.core.behavior.channel.createEmbed
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminBot @Inject constructor(
    private val build: Build,
    colorPalette: ColorPalette
) : CommandHandlerBot {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    override val embedColor: Color = colorPalette.aqua

    override val handler = commandBuilder("info", CommandHandler.Type.Mention) {
        hiddenHelp = true
        action(withMessage = false) {
            buildInfo()
        }
    }

    private suspend fun CommandHandlerEvent.buildInfo() {
        if (!build.valid) {
            channel.createMessage("Invalid build info; source not built from git repo")
            return
        }
        channel.createEmbed {
            color = embedColor
            title = "Build Info"
            field {
                name = "Version"
                value = buildString { appendLink(build.version, build.commitUrl) }
            }
            field {
                name = "Build Time"
                value = build.buildTime
            }
        }
    }
}

@Module(includes = [CoreModule::class, BotPrefixModule::class])
object AdminBotModule {

    @Provides
    @IntoSet
    @Singleton
    fun bot(bot: AdminBot): CommandHandlerBot = bot
}
