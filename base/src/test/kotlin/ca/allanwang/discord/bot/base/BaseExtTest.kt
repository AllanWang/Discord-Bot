package ca.allanwang.discord.bot.base

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class BaseExtTest {

    @Test
    fun chunkBySize() {
        val chunk = "abcde"
        val source = MutableList(5) { chunk }

        assertThat(source.chunkedByLength(12), equalTo(listOf("$chunk\n$chunk", "$chunk\n$chunk", chunk)))
    }

    @Test
    fun chunkBySizeEmpty() {
        assertThat(emptyList<String>().chunkedByLength(emptyText = "Empty Text"), equalTo(listOf("Empty Text")))
    }
}
