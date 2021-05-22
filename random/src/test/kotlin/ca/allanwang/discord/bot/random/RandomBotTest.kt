package ca.allanwang.discord.bot.random

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RandomBotTest {

    @Test
    fun rangeTest() {
        fun assertRangeEquals(range: Pair<Int, Int>?, input: String) =
            assertEquals(range, DiceBot.rollRange(input), range?.toString())

        assertRangeEquals(2 to 10, "2 10")
        assertRangeEquals(1 to 8, " 8 ")
        assertRangeEquals(null, "1 hello 2")
    }
}
