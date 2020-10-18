package ca.allanwang.discord.bot.qotd

import ca.allanwang.discord.bot.firebase.FirebaseRootRef
import com.gitlab.kordlib.common.entity.Embed
import com.gitlab.kordlib.common.entity.Snowflake
import com.google.firebase.database.DatabaseReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Qotd @Inject constructor(
    @FirebaseRootRef rootRef: DatabaseReference
) {
    suspend fun getEmbed(group: Snowflake, message: String): Embed {
        TODO()
    }

    private suspend fun getImage(group: Snowflake): String? {
        return null
    }

    private suspend fun getTemplate(group: Snowflake): String? {
        return null
    }

    private suspend fun getChannel(group: Snowflake): Snowflake? {
        return null
    }
}