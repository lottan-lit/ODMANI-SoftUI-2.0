package br.com.lottan.nativeapp.domain

import java.time.*

object ReadingRules {
    fun activeMillis(start: Long, end: Long, interaction: Long): Long =
        (minOf(end, interaction + 120000L) - start).coerceIn(0L, (end - start).coerceAtLeast(0L))

    fun chartMaximum(values: List<Long>, time: Boolean): Long =
        (values.maxOrNull() ?: 0L).coerceAtLeast(if (time) 1800000L else 30L)

    const val MAX_PDF_BYTES = 200L * 1024 * 1024

    fun title(name: String) =
        name.replace(Regex("(?i)\\.pdf$"), "").replace('_', ' ').replace(Regex("\\s+"), " ").trim()

    fun page(value: Int, total: Int) = value.coerceIn(0, (total - 1).coerceAtLeast(0))

    fun level(seconds: Long) =
        when {
            seconds <= 0 -> 0
            seconds < 300 -> 1
            seconds < 900 -> 2
            seconds < 1800 -> 3
            else -> 4
        }

    /** Split by local midnight, including timezone/DST boundaries; milliseconds are retained. */
    fun split(start: Instant, end: Instant, zone: ZoneId): Map<String, Long> {
        val result = linkedMapOf<String, Long>()
        var cursor = start
        while (cursor < end) {
            val day = cursor.atZone(zone).toLocalDate()
            val boundary = day.plusDays(1).atStartOfDay(zone).toInstant()
            val stop = minOf(boundary, end)
            result[day.toString()] = Duration.between(cursor, stop).toMillis()
            cursor = stop
        }
        return result
    }
}
