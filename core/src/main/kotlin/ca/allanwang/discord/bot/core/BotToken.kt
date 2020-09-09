package ca.allanwang.discord.bot.core

import com.gitlab.kordlib.core.Kord
import com.google.common.flogger.FluentLogger
import java.io.File
import java.io.FileInputStream
import java.util.*

interface BotFeature {

    suspend fun init(): Boolean = true

    suspend fun Kord.attach()
}

suspend fun Kord.Companion.create(args: Array<String>): Kord {
    val token = BotToken.fromArgs(args) ?: BotToken.fromProperties() ?: error("missing bot token")
    return Kord(token)
}

object BotToken {

    private val logger = FluentLogger.forEnclosingClass()

    fun fromArgs(args: Array<String>): String? = args.firstOrNull()?.also {
        logger.atInfo().log("Got token from args")
    }

    fun fromProperties(key: String = "bot_token", path: String = "priv.properties"): String? {
        val prop = Properties()
        val file = File(path)
        if (!file.isFile) return null
        FileInputStream(file).use { prop.load(it) }
        val token = prop.getProperty(key) ?: return null
        logger.atInfo().log("Got token from properties")
        return token
    }

}