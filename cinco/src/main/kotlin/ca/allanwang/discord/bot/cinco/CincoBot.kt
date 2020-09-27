package ca.allanwang.discord.bot.cinco


import ca.allanwang.discord.bot.base.*
import ca.allanwang.discord.bot.cinco.game.CincoGame
import ca.allanwang.discord.bot.cinco.game.WordBank
import ca.allanwang.discord.bot.cinco.game.CincoContext
import ca.allanwang.discord.bot.cinco.game.features.CincoGameFeatureModule
import ca.allanwang.discord.bot.cinco.game.features.CincoVariant
import com.gitlab.kordlib.core.behavior.channel.MessageChannelBehavior
import com.gitlab.kordlib.core.behavior.channel.createEmbed
import com.gitlab.kordlib.core.behavior.edit
import com.gitlab.kordlib.core.entity.ReactionEmoji
import com.gitlab.kordlib.core.entity.User
import com.gitlab.kordlib.kordx.emoji.Emojis
import com.gitlab.kordlib.kordx.emoji.toReaction
import com.google.common.flogger.FluentLogger
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toSet
import kotlinx.coroutines.launch
import javax.inject.*

@Singleton
class CincoBot @Inject constructor(
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

    private suspend fun CommandHandlerEvent.selectVariant() {
        logger.atInfo().log("Select cinco variant")
        startVariant(CincoVariant.Azul)
    }

    private suspend fun CommandHandlerEvent.startVariant(variant: CincoVariant) {
        logger.atInfo().log("Start cinco %s", variant.tag)
        val baseDescription = "React to participate!"
        val message = channel.createEmbed {
            color = variant.color
            title = "Cinco ${variant.name}"
            description = baseDescription
        }
        message.addReaction(participationEmoji)
        val secondsToWait = 15
        delay((secondsToWait - 10) * 1000L)
        (10 downTo 1).forEach { countdown ->
            message.kord.launch {
                message.edit {
                    embed {
                        color = variant.color
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
        val participants = channel.getMessage(message.id).getReactors(participationEmoji)
            .filter { it.isBot != true }
            .toSet()
        logger.atInfo().log("Participants cinco %s: %s", variant, participants)

        if (participants.isEmpty()) {
            channel.createEmbed {
                color = variant.color
                title = "Cancelled"
                description = "No participants; game cancelled"
            }
            return
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
        component.game().start()
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