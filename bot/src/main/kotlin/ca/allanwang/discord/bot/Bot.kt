package ca.allanwang.discord.bot

import ca.allanwang.discord.bot.echo.echoBot
import com.gitlab.kordlib.core.Kord
import java.io.File
import java.io.FileInputStream
import java.util.*

suspend fun main(args: Array<String>) {

    val token = args.firstOrNull() ?: run {
        val prop = Properties()
        val file = File("priv.properties")
        if (!file.isFile) return@run null
        FileInputStream(file).use { prop.load(it) }
        prop.getProperty("bot_token")
    } ?: error("bot token required")

    println("Initialized Bot")

    val kord = Kord(token)

    kord.echoBot()

    kord.login { playing("!ping to pong") }
}
