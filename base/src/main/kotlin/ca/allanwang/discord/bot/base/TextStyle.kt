package ca.allanwang.discord.bot.base

inline fun StringBuilder.appendOptional(
    enable: Boolean,
    style: (() -> Unit) -> Unit,
    noinline block: () -> Unit
) {
    if (enable) style(block)
    else block()
}

inline fun StringBuilder.appendLink(text: String, url: String) {
    append("[")
    append(text)
    append("]")
    append("(")
    append(url)
    append(")")
}

inline fun StringBuilder.appendBold(block: () -> Unit) {
    append("**")
    block()
    append("**")
}

inline fun StringBuilder.appendItalic(block: () -> Unit) {
    append("*")
    block()
    append("*")
}

inline fun StringBuilder.appendUnderline(block: () -> Unit) {
    append("__")
    block()
    append("__")
}

inline fun StringBuilder.appendCodeBlock(block: () -> Unit) {
    append("`")
    block()
    append("`")
}

inline fun StringBuilder.appendBigCodeBlock(block: () -> Unit) {
    append("```")
    block()
    append("```")
}

inline fun StringBuilder.appendQuote(block: () -> Unit) {
    append("> ")
    block()
}

/**
 * Basic plural support.
 */
inline fun StringBuilder.appendPlural(count: Int, single: String, plural: String = "${single}s") =
    append(if (count == 1) "1 $single" else "$count $plural")