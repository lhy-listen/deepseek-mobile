package com.lhy.deepseekmobile.util

/** Cheap, dependency-free token estimate: CJK ~1 token/char, latin ~3.5 chars/token. */
object Tokens {

    fun estimate(text: String): Int {
        if (text.isEmpty()) return 0
        var cjk = 0
        var other = 0
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            when {
                cp in 0x4E00..0x9FFF || cp in 0x3400..0x4DBF || cp in 0x3000..0x303F ||
                    cp in 0xFF00..0xFFEF || cp in 0x3040..0x30FF || cp in 0xAC00..0xD7AF -> cjk++
                else -> other++
            }
            i += Character.charCount(cp)
        }
        return cjk + (other / 3.5).toInt() + 4
    }

    fun format(count: Int): String = when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fk", count / 1_000.0)
        else -> count.toString()
    }
}
