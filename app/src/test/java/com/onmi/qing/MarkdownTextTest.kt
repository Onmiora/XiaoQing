package com.onmi.qing

import com.onmi.qing.ui.components.parseMarkdown
import org.junit.Test

class MarkdownTextTest {
    @Test
    fun testBold() {
        val result = parseMarkdown("**bold**")
        println("Test bold: **bold** -> ${result.text}")
    }
}
