package ca.allanwang.discord.bot.cinco.game.core

import ca.allanwang.discord.bot.base.appendBold
import ca.allanwang.discord.bot.cinco.CincoPlayers
import ca.allanwang.discord.bot.cinco.CincoScope
import ca.allanwang.discord.bot.cinco.game.features.CincoVariant
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.EmbedBuilder
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject

@CincoScope
class CincoMessageBehavior @Inject constructor(
    private val kord: Kord,
    private val messageChannelBehavior: MessageChannelBehavior,
    @CincoPlayers private val players: Set<User>,
    private val variant: CincoVariant,
    private val pointTracker: CincoPointTracker
) {

    private fun MessageCreateEvent.toCincoEntry(): CincoEntry? {
        val word = message.content
        if (word.length != 5 || word.contains(' ')) return null
        val player = message.author
        if (player == null || player !in players) return null
        return CincoEntry(player = player, word = word.toLowerCase(Locale.US))
    }

    suspend fun createCincoEmbed(builder: EmbedBuilder.() -> Unit): Flow<CincoEntry> {
        createEmbed(builder)
        return playerMessageFlow()
            .mapNotNull {
                it.toCincoEntry()
            }
    }

    fun playerMessageFlow(): Flow<MessageCreateEvent> =
        kord.events.drop(1).filterIsInstance<MessageCreateEvent>()
            .filter { it.message.channelId == messageChannelBehavior.id }
            .filter { it.message.author?.isBot != true }
            .filter { it.message.author in players }

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