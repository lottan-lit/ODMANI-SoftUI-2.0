package br.com.lottan.nativeapp

import androidx.compose.ui.graphics.Color
import br.com.lottan.nativeapp.ui.VisualTokens
import kotlin.math.pow
import org.junit.Assert.*
import org.junit.Test

class VisualTokensTest {
    private fun luminance(c: Color): Double {
        fun channel(v: Float) = if (v <= .04045f) v / 12.92 else ((v + .055) / 1.055).pow(2.4)
        return channel(c.red) * .2126 + channel(c.green) * .7152 + channel(c.blue) * .0722
    }

    private fun contrast(a: Color, b: Color): Double {
        val x = luminance(a)
        val y = luminance(b)
        return (maxOf(x, y) + .05) / (minOf(x, y) + .05)
    }

    @Test
    fun textAndButtonContrastInBothThemes() {
        for (p in listOf(VisualTokens.Light, VisualTokens.Dark)) {
            assertTrue(contrast(p.text, p.background) >= 4.5)
            assertTrue(contrast(p.muted, p.background) >= 4.5)
            assertTrue(contrast(p.muted, p.surface) >= 4.5)
            assertTrue(contrast(p.primary, p.onPrimary) >= 4.5)
        }
    }

    @Test
    fun exactBackgroundsAndSeparateSurfaces() {
        assertEquals(Color(0xffF5F6F8), VisualTokens.Light.background)
        assertEquals(Color(0xff141416), VisualTokens.Dark.background)
        assertNotEquals(VisualTokens.Dark.background, VisualTokens.Dark.surface)
    }
}
