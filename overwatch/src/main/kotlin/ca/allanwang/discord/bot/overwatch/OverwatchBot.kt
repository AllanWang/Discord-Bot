package ca.allanwang.discord.bot.overwatch

import ca.allanwang.discord.bot.base.*
import com.google.common.flogger.FluentLogger
import dev.kord.common.Color
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.rest.builder.message.EmbedBuilder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OverwatchBot @Inject constructor(
    private val overwatchApi: OverwatchApi
) : CommandHandlerBot {
    companion object {
        private val logger = FluentLogger.forEnclosingClass()
        private val embedColor = Color(0xFFEE9C30.toInt())
    }

    override val handler: CommandHandler = commandBuilder(CommandHandler.Type.Prefix) {
        arg("ow") {
            arg("link") {
                action(withMessage = true) {
                    link()
                }
            }
            arg("stats") {
                action(withMessage = false) {
                    stats()
                }
                arg("cached") {
                    action(withMessage = false) {
                        stats(cacheOnly = true)
                    }
                }
            }
        }
    }

    private suspend fun CommandHandlerEvent.link() {
        val authorId = authorId ?: return
        val tag = message.trim()
        val data = overwatchApi.parseUserData(tag)
        if (data == null) {
            channel.createMessage("Could not find account info from ${overwatchApi.userDataUrl(tag)}")
        } else {
            channel.createMessage(data.toString())
            overwatchApi.saveUserData(authorId, data)
        }
    }

    private suspend fun CommandHandlerEvent.stats(cacheOnly: Boolean = false) {
        val authorId = authorId ?: return
        val data: OverwatchFullData? =
            if (cacheOnly) {
                val user = overwatchApi.getUserData(authorId)?.takeIf { it.isComplete }
                OverwatchFullData(old = user, new = user)
            } else {
                val data = overwatchApi.getFullUserData(authorId)
                data?.new?.let { newUser ->
                    overwatchApi.saveUserData(authorId, newUser)
                }
                data
            }
        showData(data)
    }

    private suspend fun CommandHandlerEvent.showData(data: OverwatchFullData?) {
        val user = data?.new ?: data?.old
        if (user == null) {
            channel.createMessage(
                buildString {
                    append("Could not find user data; add your account via ")
                    appendCodeBlock { append("${prefix}ow link [case sensitive battletag]") }
                }
            )
            return
        }
        val delta = data?.delta
        channel.createEmbed {
            color = embedColor
            author {
                name = user.name.uppercase()
                url = overwatchApi.userDataUrl(user.tag)
                icon = user.portraitUrl
            }
            title = "Quick Play Stats"

            field("Level", user.level.toString(), delta?.level?.leadingFormat())
            field(
                "Endorsement",
                user.endorsementLevel.toString(),
                delta?.endorsementLevel?.leadingFormat(),
                inline = true
            )

            gameStats(user.quickPlay, delta?.quickPlay)

            footer {
                text = user.tag
            }
        }
    }

    private fun EmbedBuilder.field(tag: String, value: String, delta: String? = null, inline: Boolean = false) {
        field {
            this.inline = inline
            name = tag
            this.value = buildString {
                append(value)
                if (delta != null) {
                    append(" ($delta)")
                }
            }
        }
    }

    private fun EmbedBuilder.gameStats(gameStats: OverwatchUser.GameStats, delta: OverwatchDelta.GameStatsDelta?) {
        field("Wins", gameStats.wins.toString(), delta?.wins?.leadingFormat())
        field("Losses", gameStats.losses.toString(), delta?.losses?.leadingFormat(), inline = true)
        field("Win Rate", gameStats.winRate.percentFormat(), delta?.winRate?.percentFormat(leadingSign = true))
    }

    private fun Int.leadingFormat(): String = "%+d".format(this)

    private fun Float.percentFormat(leadingSign: Boolean = false): String =
        "%${if (leadingSign) "+" else ""}.2f%%".format(this * 100)
}
