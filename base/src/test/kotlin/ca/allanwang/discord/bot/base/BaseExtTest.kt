package ca.allanwang.discord.bot.base

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class BaseExtTest {

    @Test
    fun chunkBySize() {
        val chunk = "abcde"
        val source = MutableList(5) { chunk }

        assertThat(source.chunkedByLength(12)).isEqualTo(listOf("$chunk\n$chunk", "$chunk\n$chunk", chunk))
    }

    @Test
    fun chunkBySizeEmpty() {
        assertThat(emptyList<String>().chunkedByLength(emptyText = "Empty Text")).isEqualTo(listOf("Empty Text"))
    }
}
