package ca.allanwang.discord.bot.base

import ca.allanwang.discord.bot.core.withTimeout
import com.gitlab.kordlib.common.entity.Snowflake
import com.gitlab.kordlib.core.entity.Message
import com.gitlab.kordlib.core.event.message.ReactionAddEvent
import com.gitlab.kordlib.kordx.emoji.Emojis
import com.gitlab.kordlib.kordx.emoji.toReaction
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import java.util.concurrent.TimeUnit

suspend fun Message.confirmationReaction(user: Snowflake? = null): Boolean {
    val positive = Emojis.whiteCheckMark.toReaction()
    val negative = Emojis.negativeSquaredCrossMark.toReaction()
    addReaction(positive)
    addReaction(negative)
    return kord.events.filterIsInstance<ReactionAddEvent>()
        .run { if (user == null) this else filter { it.userId == user } }
        .mapNotNull {
            when (it.emoji) {
                positive -> true
                negative -> false
                else -> null
            }
        }
        .withTimeout(TimeUnit.MINUTES.toMillis(1))
        .first()
}