package br.com.lottan.nativeapp

import br.com.lottan.nativeapp.domain.ReadingRules as R
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class ReadingRulesTest {
    @Test
    fun pageLimits() {
        assertEquals(0, R.page(-5, 58))
        assertEquals(57, R.page(900, 58))
        assertEquals(0, R.page(4, 0))
    }

    @Test
    fun safeTitle() {
        assertEquals("Temos Todo Tempo", R.title("Temos_Todo_Tempo.PDF"))
        assertEquals("a <b>", R.title("a_<b>.pdf"))
    }

    @Test
    fun intensityBoundaries() {
        assertEquals(
            listOf(0, 1, 1, 2, 2, 3, 3, 4),
            listOf(0L, 1, 299, 300, 899, 900, 1799, 1800).map(R::level),
        )
    }

    @Test
    fun midnightSplit() {
        val zone = ZoneId.of("America/Sao_Paulo")
        val start = LocalDateTime.parse("2026-10-24T23:59:59.500").atZone(zone).toInstant()
        val parts = R.split(start, start.plusMillis(1500), zone)
        assertEquals(500L, parts["2026-10-24"])
        assertEquals(1000L, parts["2026-10-25"])
        assertEquals(1500L, parts.values.sum())
    }

    @Test
    fun daylightSavingSplit() {
        val zone = ZoneId.of("America/New_York")
        val start = LocalDate.of(2026, 3, 8).atStartOfDay(zone).toInstant()
        val end = LocalDate.of(2026, 3, 9).atStartOfDay(zone).toInstant()
        assertEquals(23 * 3600000L, R.split(start, end, zone).values.sum())
    }

    @Test
    fun invalidIntervalsDoNotInventActivity() {
        val now = Instant.now()
        assertTrue(R.split(now, now.minusSeconds(1), ZoneOffset.UTC).isEmpty())
    }
}
