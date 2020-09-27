package ca.allanwang.discord.bot.cinco


import ca.allanwang.discord.bot.base.*
import ca.allanwang.discord.bot.cinco.game.CincoContext
import ca.allanwang.discord.bot.cinco.game.CincoGame
import ca.allanwang.discord.bot.cinco.game.WordBank
import ca.allanwang.discord.bot.cinco.game.features.CincoGameFeatureModule
import ca.allanwang.discord.bot.cinco.game.features.CincoVariant
import com.gitlab.kordlib.common.entity.Snowflake
import com.gitlab.kordlib.core.Kord
import com.gitlab.kordlib.core.behavior.channel.MessageChannelBehavior
import com.gitlab.kordlib.core.behavior.channel.createEmbed
import com.gitlab.kordlib.core.behavior.edit
import com.gitlab.kordlib.core.entity.ReactionEmoji
import com.gitlab.kordlib.core.entity.User
import com.gitlab.kordlib.kordx.emoji.Emojis
import com.gitlab.kordlib.kordx.emoji.toReaction
import com.gitlab.kordlib.rest.builder.message.EmbedBuilder
import com.google.common.flogger.FluentLogger
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.*

@Singleton
class CincoBot @Inject constructor(
    private val kord: Kord,
    private val cincoProvider: Provider<CincoComponent.Builder>,
    private val wordBank: WordBank
) : CommandHandlerBot {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()

        private val participationEmoji: ReactionEmoji = Emojis.whiteCheckMark.toReaction()
    }

    override val handler = commandBuilder(CommandHandler.Type.Prefix) {
        arg("cinco") {
            action(withMessage = false) {
                selectVariant()
            }
            CincoVariant.values().forEach {
                arg(it.tag) {
                    action(withMessage = false) {
                        startVariant(it)
                    }
                }
            }
            arg("check") {
                action(withMessage = true) {
                    checkWord()
                }
            }
        }
    }

    private suspend fun CommandHandlerEvent.checkWord() {
        val word = message.trim()
        val isWord = wordBank.isWord(word)
        channel.createMessage(buildString {
            appendCodeBlock {
                append(word)
            }
            if (isWord) append(" is a registered word")
            else append(" is not a registered word")
        })
    }

    private val games: MutableMap<Snowflake, Long> = ConcurrentHashMap()

    private suspend fun CommandHandlerEvent.existingGame(): Boolean {
        if (games.containsKey(event.message.channelId)) {
            logger.atInfo().log("Skipping as there is an existing game")
            channel.createMessage("Cinco is already running in this channel")
            return true
        }
        return false
    }

    private suspend fun CommandHandlerEvent.selectVariant() {
        if (existingGame()) return
        logger.atInfo().log("Select cinco variant")
        startVariant(CincoVariant.Azul)
    }

    private suspend fun CommandHandlerEvent.getParticipants(variant: CincoVariant): Set<User> {
        fun EmbedBuilder.base() {
            color = variant.color
            title = "Cinco ${variant.name}"
        }

        val baseDescription = buildString {
            append(variant.description)
            append("\n\n")
            append("React to participate!")
        }

        val message = channel.createEmbed {
            base()
            description = baseDescription
        }
        message.addReaction(participationEmoji)
        val secondsToWait = 15
        delay((secondsToWait - 10) * 1000L)
        (10 downTo 1).forEach { countdown ->
            message.kord.launch {
                message.edit {
                    embed {
                        base()
                        description = buildString {
                            append(baseDescription)
                            append("\n\n")
                            appendBold {
                                append(countdown)
                            }
                        }
                    }
                }
            }
            delay(1000)
        }
        return channel.getMessage(message.id).getReactors(participationEmoji)
            .filter { it.isBot != true }
            .toSet()
    }

    private suspend fun CommandHandlerEvent.startVariant(variant: CincoVariant) {
        if (existingGame()) return
        logger.atInfo().log("Start cinco %s", variant.tag)
        games[event.message.channelId] = System.currentTimeMillis()

        kord.launch(CoroutineExceptionHandler { _, throwable ->
            logger.atWarning().withCause(throwable).log("Cinco error")
            games.remove(event.message.channelId)
        }) {
            val participants = getParticipants(variant)

            logger.atInfo().log("Participants cinco %s: %s", variant, participants)

            if (participants.isEmpty()) {
                channel.createEmbed {
                    color = variant.color
                    title = "Cancelled"
                    description = "No participants; game cancelled"
                }
                return@launch
            }

            val component = cincoProvider.get()
                .channel(channel)
                .variant(CincoVariant.Azul)
                .players(participants)
                .context(
                    CincoContext(
                        botPrefix = prefix,
                        gameRounds = 15,
                        roundTimeout = 30_000L
                    )
                )
                .build()

            component.game().play()
        }.invokeOnCompletion {
            games.remove(event.message.channelId)
        }
    }
}

@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class CincoScope

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class CincoPlayers

@Module(subcomponents = [CincoComponent::class])
interface CincoModule

@CincoScope
@Subcomponent(modules = [CincoGameFeatureModule::class])
interface CincoComponent {

    @CincoScope
    fun game(): CincoGame

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun channel(channel: MessageChannelBehavior): Builder

        @BindsInstance
        fun variant(variant: CincoVariant): Builder

        @BindsInstance
        fun players(@CincoPlayers players: Set<User>): Builder

        @BindsInstance
        fun context(context: CincoContext): Builder

        fun build(): CincoComponent
    }
}