package ca.allanwang.discord.bot.cinco.game.features

import dev.kord.common.Color
import java.util.*

enum class CincoVariant(color: Int, val description: String) {
    Azul(color = 0xFF0063C6.toInt(), description = "Unscramble the word");

    val tag: String = name.toLowerCase(Locale.US)
    val color = Color(color)
}