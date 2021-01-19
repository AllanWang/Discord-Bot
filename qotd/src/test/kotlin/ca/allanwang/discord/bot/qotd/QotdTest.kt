package ca.allanwang.discord.bot.qotd

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class QotdTest {
    /**
     * Note that test time intervals should be > 5000 as thresholds are taken into account
     */
    @Test
    fun timeTest_qotdNow() {
        assertThat(Qotd.newTime(110_000, 100_000, 10_000), equalTo(110_000))
        assertThat(Qotd.newTime(110_000, 100_001, 6000), equalTo(110_000))
        assertThat(Qotd.newTime(110_000, 99_999, 6000), equalTo(110_000))
    }

    @Test
    fun timeTest_qotdLater() {
        assertThat(Qotd.newTime(11_500, 200, 6_000), equalTo(5_500))
        assertThat(Qotd.newTime(90_000_000, 6_000, 100_000), equalTo(100_000))
    }

    @Test
    fun timeTest_qotdBefore() {
        assertThat(Qotd.newTime(11_500, 20_200, 6_000), equalTo(23_500))
    }
}