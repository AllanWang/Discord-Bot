package ca.allanwang.discord.bot.base

import dev.kord.common.Color
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ColorPalette @Inject internal constructor() {
    val orange: Color = Color(0xfff47720.toInt())
    val gold: Color = Color(0xFFEEB501.toInt())
    val blue: Color = Color(0xff306EA4.toInt())
    val lightBlue: Color = Color(0xFF03a5fc.toInt())
    val aqua: Color = Color(0xFF4DB6C1.toInt())
    val green: Color = Color(0xff599E70.toInt())
    val lavendar: Color = Color(0xFFAB98D2.toInt())
    val red: Color = Color(0xFFDC1E28.toInt())

    // Orange
    val overwatch: Color = Color(0xFFEE9C30.toInt())

    val default: Color = blue
}
