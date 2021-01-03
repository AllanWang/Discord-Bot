package ca.allanwang.discord.bot.base

import ca.allanwang.discord.bot.core.CoreModule
import ca.allanwang.discord.bot.firebase.FirebaseModule
import dev.kord.core.event.message.MessageCreateEvent
import dagger.Subcomponent
import javax.inject.Scope

@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class MessageScope

@Subcomponent(modules = [CoreModule::class, FirebaseModule::class])
@MessageScope
interface MessageComponent {

    @Subcomponent.Builder
    interface Builder {
        fun message(message: MessageCreateEvent): Builder

        fun build(): MessageComponent
    }
}