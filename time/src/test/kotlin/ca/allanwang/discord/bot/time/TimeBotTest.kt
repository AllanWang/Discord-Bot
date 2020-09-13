package ca.allanwang.discord.bot.time

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TimeBotTest {

    /**
     * Basic test to verify regex validity.
     *
     * Note that matching a regex parse does not necessarily constitute a time request.
     * The regex currently accepts generic inputs such as just a number (eg 8)
     */
    @Test
    fun regexTest() {
        val regex = TimeBot.timeRegex

        fun assertTime(expected: String, vararg candidates: String) {
            candidates.forEach { candidate ->
                fun fail(throwable: Throwable? = null): Nothing =
                    fail("Regex test failed with $candidate; expected $expected", throwable)

                val match = regex.matchEntire(candidate) ?: fail()
                runCatching {
                    val result = buildString {
                        append(match.groupValues[1].trimStart('0'))
                        append(':')
                        append(match.groupValues[2].takeIf { it.isNotEmpty() } ?: "00")
                        append(' ')
                        append(match.groupValues[3].takeIf { it.isNotEmpty() }?.toLowerCase() ?: "am")
                    }
                    assertEquals(expected, result)
                }.onFailure {
                    fail(it)
                }
            }
        }

        assertTime("8:00 pm", "8pm", "8 PM", "8:00\tPm")
        assertTime("3:12 am", "3:12am", "3:12", "03:12")
        assertTime("1:00 am", "1", "1am", "1:00am")
    }

    @Test
    fun badRegexTest() {
        val regex = TimeBot.timeRegex

        listOf(
            "8:0",
            "23:00",
            "1234",
            "8:pm"
        ).forEach {
            assertFalse(regex.matches(it), message = "Found unexpected match in $it")
        }
    }

}