package br.com.lottan.nativeapp
import br.com.lottan.nativeapp.domain.ReadingRules
import org.junit.Assert.*
import org.junit.Test
class ActivityRegressionTest {
    @Test fun smallValuesDoNotFillChart() {
        assertEquals(1800000L, ReadingRules.chartMaximum(listOf(0, 15000, 0), true))
        assertEquals(30L, ReadingRules.chartMaximum(listOf(0, 1, 0), false))
        assertEquals(0.5, 15_000.0 / ReadingRules.chartMaximum(listOf(15000), true) * 60, 0.001)
    }
    @Test fun emptyAndLargePeriodsHaveValidScale() {
        assertEquals(30L, ReadingRules.chartMaximum(emptyList(), false))
        assertEquals(90L, ReadingRules.chartMaximum(listOf(1, 90), false))
        assertEquals(3600000L, ReadingRules.chartMaximum(listOf(3600000), true))
    }
    @Test fun inactivityIsNotCounted() {
        assertEquals(15000L, ReadingRules.activeMillis(0, 15000, 0))
        assertEquals(5000L, ReadingRules.activeMillis(115000, 130000, 0))
        assertEquals(0L, ReadingRules.activeMillis(130000, 145000, 0))
        assertEquals(15000L, ReadingRules.activeMillis(130000, 145000, 140000))
    }
}
