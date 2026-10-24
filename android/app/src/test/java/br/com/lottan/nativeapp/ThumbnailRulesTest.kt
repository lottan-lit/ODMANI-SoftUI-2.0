package br.com.lottan.nativeapp

import br.com.lottan.nativeapp.domain.ThumbnailRules as Rules
import org.junit.Assert.*
import org.junit.Test

class ThumbnailRulesTest {
    @Test
    fun keepsPortraitLandscapeSquareAndExtremeRatios() {
        for ((w, h) in listOf(600 to 800, 800 to 600, 100 to 100, 100 to 100000, 100000 to 100)) {
            val (x, y) = Rules.size(w, h)
            assertTrue(x in 1..Rules.MAX_EDGE && y in 1..Rules.MAX_EDGE)
            assertTrue(x.toLong() * y <= Rules.MAX_PIXELS)
            // Integer rounding error is at most one output pixel per dimension.
            assertTrue(kotlin.math.abs(x.toDouble() / w - y.toDouble() / h) <= 1.0 / w + 1.0 / h)
        }
    }

    @Test
    fun invalidatesWhenFileChanges() {
        val base = Rules.key("id", 100, 200)
        assertNotEquals(base, Rules.key("id", 101, 200))
        assertNotEquals(base, Rules.key("id", 100, 201))
        assertEquals(base, Rules.key("id", 100, 200))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidDimensions() {
        Rules.size(0, 100)
    }
}
