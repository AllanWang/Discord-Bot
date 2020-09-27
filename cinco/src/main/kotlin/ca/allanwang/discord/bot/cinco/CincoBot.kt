package ca.allanwang.discord.bot.cinco


import ca.allanwang.discord.bot.base.CommandHandler
import ca.allanwang.discord.bot.base.CommandHandlerBot
import ca.allanwang.discord.bot.base.CommandHandlerEvent
import ca.allanwang.discord.bot.base.commandBuilder
import ca.allanwang.discord.bot.cinco.game.CincoGame
import ca.allanwang.discord.bot.cinco.game.CincoGameModule
import ca.allanwang.discord.bot.cinco.game.CincoVariant
import com.gitlab.kordlib.common.entity.Snowflake
import com.gitlab.kordlib.core.behavior.channel.MessageChannelBehavior
import com.gitlab.kordlib.core.entity.User
import com.google.common.flogger.FluentLogger
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent
import javax.inject.*

@Singleton
class CincoBot @Inject constructor(
    private val cincoProvider: Provider<CincoComponent.Builder>
) : CommandHandlerBot {

    companion object {
        private val logger = FluentLogger.forEnclosingClass()
    }

    override val handler = commandBuilder(CommandHandler.Type.Prefix) {
        arg("cinco") {
            action(withMessage = false) {
                logger.atInfo().log("cinco")
                selectVariant()
            }
        }
    }

    private suspend fun CommandHandlerEvent.selectVariant() {
        val component = cincoProvider.get()
            .channel(channel)
            .variant(CincoVariant.Azul)
            .players(setOf(event.message.author!!)) // TODO update
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
@Subcomponent(modules = [CincoGameModule::class])
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

        fun build(): CincoComponent
    }
}