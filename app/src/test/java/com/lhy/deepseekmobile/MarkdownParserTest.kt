package com.lhy.deepseekmobile

import com.lhy.deepseekmobile.ui.components.MdBlock
import com.lhy.deepseekmobile.ui.components.MdParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun `parses heading paragraph and rule`() {
        val blocks = MdParser.parse("# 标题\n\n正文第一行\n正文第二行\n\n---")
        assertTrue(blocks[0] is MdBlock.Heading)
        assertEquals(1, (blocks[0] as MdBlock.Heading).level)
        assertEquals("标题", (blocks[0] as MdBlock.Heading).text)
        assertTrue(blocks[1] is MdBlock.Paragraph)
        assertEquals("正文第一行\n正文第二行", (blocks[1] as MdBlock.Paragraph).text)
        assertEquals(MdBlock.Rule, blocks[2])
    }

    @Test
    fun `parses fenced code with language`() {
        val blocks = MdParser.parse("```kotlin\nfun main() {}\n```")
        val code = blocks.single() as MdBlock.Code
        assertEquals("kotlin", code.lang)
        assertEquals("fun main() {}", code.code)
    }

    @Test
    fun `parses ordered and unordered list`() {
        val blocks = MdParser.parse("- 第一项\n- 第二项\n\n1. one\n2. two")
        val first = blocks[0] as MdBlock.ListBlock
        assertEquals(2, first.items.size)
        assertEquals("第一项", first.items[0].text)
        val second = blocks[1] as MdBlock.ListBlock
        assertTrue(second.items[0].ordered)
        assertEquals(1, second.items[0].number)
    }

    @Test
    fun `parses table`() {
        val blocks = MdParser.parse("| a | b |\n| --- | --- |\n| 1 | 2 |")
        val table = blocks.single() as MdBlock.Table
        assertEquals(listOf("a", "b"), table.header)
        assertEquals(listOf(listOf("1", "2")), table.rows)
    }

    @Test
    fun `keeps quote content`() {
        val blocks = MdParser.parse("> hello\n> world")
        assertEquals("hello\nworld", (blocks.single() as MdBlock.Quote).text)
    }
}
