package ca.allanwang.discord.bot.time

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TimeApiTest {

    /**
     * Basic test to verify regex validity.
     *
     * Note that matching a regex parse does not necessarily constitute a time request.
     * The regex currently accepts generic inputs such as just a number (eg 8)
     */
    @Test
    fun regexTest() {
        val regex = TimeApi.timeRegex

        fun assertTime(expected: String, matchEntire: Boolean = true, vararg candidates: String) {
            candidates.forEach { candidate ->
                fun fail(throwable: Throwable? = null): Nothing =
                    fail("Regex test failed with '$candidate'; expected $expected", throwable)

                val match = (if (matchEntire) regex.matchEntire(candidate) else regex.find(candidate)) ?: fail()
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

        assertTime("8:00 pm", matchEntire = true, "8pm", "8 PM", "8:00\tPm")
        assertTime("3:12 am", matchEntire = true, "3:12am", "3:12", "03:12")
        assertTime("1:00 am", matchEntire = true, "1", "1am", "1:00am")
        assertTime("1:00 am", matchEntire = false, " 1 ", "'1'", "\"1\"", "<1>", "_1_")
    }

    @Test
    fun badRegexTest() {
        val regex = TimeApi.timeRegex

        listOf(
            "8:0",
            "23:00",
            "1234",
            "8:pm",
            "8:000",
            "a8:00 "
        ).forEach {
            assertFalse(regex.matches(it), message = "Found unexpected match in $it")
        }
    }

    @Test
    fun timeEntryTest() {
        assertEquals(0, TimeApi.TimeEntry(hour = 12, minute = 59, pm = false).hour24, "12:59pm = 0:59")
        assertEquals(12, TimeApi.TimeEntry(hour = 12, minute = 59, pm = null).hour24, "12:59 = 12:59 (pm)")
        assertEquals(12, TimeApi.TimeEntry(hour = 12, minute = 59, pm = true).hour24, "12:59pm = 12:59")
    }
}
