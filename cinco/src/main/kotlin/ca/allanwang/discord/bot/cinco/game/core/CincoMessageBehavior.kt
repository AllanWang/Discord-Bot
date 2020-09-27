package ca.allanwang.discord.bot.cinco.game.core

import ca.allanwang.discord.bot.base.appendBold
import ca.allanwang.discord.bot.cinco.CincoPlayers
import ca.allanwang.discord.bot.cinco.CincoScope
import ca.allanwang.discord.bot.cinco.game.features.CincoVariant
import com.gitlab.kordlib.common.entity.Snowflake
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.behavior.channel.MessageChannelBehavior
import com.gitlab.kordlib.core.behavior.channel.createEmbed
import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.entity.User
import com.gitlab.kordlib.core.event.message.MessageCreateEvent
import com.gitlab.kordlib.rest.builder.message.EmbedBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

@CincoScope
class CincoMessageBehavior @Inject constructor(
    private val kord: Kord,
    private val messageChannelBehavior: MessageChannelBehavior,
    @CincoPlayers private val players: Set<User>,
    private val variant: CincoVariant,
    private val pointTracker: CincoPointTracker
) {

    val id: Snowflake = messageChannelBehavior.id

    private fun MessageCreateEvent.toCincoEntry(): CincoEntry? {
        val word = message.content
        if (word.length != 5 || word.contains(' ')) return null
        val player = message.author
        if (player == null || player !in players) return null
        return CincoEntry(player = player, word = word)
    }

    suspend fun createCincoEmbed(builder: EmbedBuilder.() -> Unit): Flow<CincoEntry> {
        createEmbed(builder)
        return kord.events.filterIsInstance<MessageCreateEvent>()
            .filter { it.message.channelId == messageChannelBehavior.id }
            .mapNotNull {
                it.toCincoEntry()
            }

    }

    suspend fun createEmbed(builder: EmbedBuilder.() -> Unit): Message = messageChannelBehavior.createEmbed {
        color = variant.color
        builder()
    }

    suspend fun showStandings(builder: EmbedBuilder.() -> Unit) {
        createEmbed {
            builder()
            field {
                name = "Standings"
                value = buildString {
                    pointTracker.getStandings().forEachIndexed { i, entry ->
                        appendBold {
                            append(i + 1)
                            append('.')
                        }
                        append(' ')
                        append(entry.player.mention)
                        append(": ")
                        append(entry.points)
                        append(" pts")
                        appendLine()
                    }
                }
            }
        }
    }
}

data class CincoEntry(val player: User, val word: String)