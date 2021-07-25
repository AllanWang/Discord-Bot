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
    private val overwatchApi: OverwatchApi,
    colorPalette: ColorPalette
) : CommandHandlerBot {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    override val embedColor: Color = colorPalette.overwatch

    override val handler: CommandHandler =
        commandBuilder("ow", CommandHandler.Type.Prefix, description = "Overwatch stats") {
            arg("link") {
                action(
                    withMessage = true, helpArgs = "[battletag]",
                    help = {
                        "Link battletag (case sensitive, eg example#1234)"
                    }
                ) {
                    link()
                }
            }
            arg("stats") {
                action(withMessage = false, help = { "View player stats" }) {
                    stats()
                }
                arg("cached") {
                    action(
                        withMessage = false,
                        help = { "View player stats from cache only; does not query playOverwatch" }
                    ) {
                        stats(cacheOnly = true)
                    }
                }
            }
        }

    private suspend fun CommandHandlerEvent.link() {
        val authorId = authorId ?: return
        val tag = message.trim()
        channel.type()
        val user = overwatchApi.parseUserData(tag)?.takeIf { it.isComplete }
        if (user == null) {
            channel.createMessage("Could not find account info from ${overwatchApi.userDataUrl(tag)}")
        } else {
            overwatchApi.saveUserData(authorId, user)
            showData(OverwatchFullData(old = user, new = user))
        }
    }

    private suspend fun CommandHandlerEvent.stats(cacheOnly: Boolean = false) {
        val authorId = authorId ?: return
        val data: OverwatchFullData? =
            if (cacheOnly) {
                val user = overwatchApi.getUserData(authorId)?.takeIf { it.isComplete }
                OverwatchFullData(old = user, new = user)
            } else {
                channel.type()
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

            field("Level", user.level.toString(), delta?.level?.deltaFormat())
            field(
                "Endorsement",
                user.endorsementLevel.toString(),
                null, // Don't show endorsement delta
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
        field("Wins", gameStats.wins.toString(), delta?.wins?.deltaFormat())
        field("Losses", gameStats.losses.toString(), delta?.losses?.deltaFormat(), inline = true)
        field("Win Rate", gameStats.winRate.percentFormat(), delta?.winRate?.deltaFormat())
    }

    private fun Int.deltaFormat(): String? = if (this == 0) null else "%+d".format(this)

    private fun Float.deltaFormat(): String? = if (this == 0f) null else "%+.2f%%".format(this * 100)

    private fun Float.percentFormat(): String = "%.2f%%".format(this * 100)
}
