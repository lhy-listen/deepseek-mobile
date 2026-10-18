package com.lhy.deepseekmobile

import com.lhy.deepseekmobile.util.Tokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TokensTest {

    @Test
    fun `cjk counts roughly one token per char`() {
        // 4 CJK chars + 4 fixed overhead
        assertEquals(8, Tokens.estimate("你好世界"))
    }

    @Test
    fun `latin is cheaper per character`() {
        // ~29 latin chars ≈ 8 tokens, plus 4 fixed overhead
        assertTrue(Tokens.estimate("hello world, this is a sentence") <= 12)
    }

    @Test
    fun `format uses k`() {
        assertEquals("1.5k", Tokens.format(1500))
    }
}
