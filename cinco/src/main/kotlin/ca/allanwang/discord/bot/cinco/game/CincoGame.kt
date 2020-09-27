package ca.allanwang.discord.bot.cinco.game

import java.awt.Color
import java.util.*

enum class CincoVariant(color: String, val description: String) {
    Azul(color = "#0063C6", description = "Unscramble the word");

    val tag: String = name.toLowerCase(Locale.US)
    val color: Color = Color.decode(color)
}

interface CincoGame {
    suspend fun start()
}