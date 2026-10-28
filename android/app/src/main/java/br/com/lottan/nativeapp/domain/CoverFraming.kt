package br.com.lottan.nativeapp.domain

import kotlin.math.*

/**
 * Presentation-only framing for library covers. The PDF, its pages and its thumbnail pixels are never modified:
 * this only decides which part of the already rendered thumbnail is shown inside a uniform card.
 */
object CoverFraming {
    /** Rectangle in thumbnail pixels: [left, right) x [top, bottom). */
    data class Box(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        val width get() = right - left
        val height get() = bottom - top
    }

    /** Result of analysing a thumbnail once: the real content area (without uniform page margins) and an edge colour. */
    data class Analysis(val width: Int, val height: Int, val content: Box, val edgeColor: Int)

    /** How the card is filled: [window] of the thumbnail covers the whole card, or the whole thumbnail is fitted over a backdrop. */
    data class Plan(val fill: Boolean, val window: Box)

    private const val TOLERANCE = 28 // sum of per-channel differences still considered "same colour"
    private const val MAX_MARGIN_REMOVAL = .40f // never treat more than 40% of an axis as margin
    private const val EDGE_KEEP = .012f // breathing room kept around detected content
    private const val MAX_CUT = .08f // largest fraction of the cover that may be cropped away when no margin can absorb it

    private fun red(c: Int) = c shr 16 and 255
    private fun green(c: Int) = c shr 8 and 255
    private fun blue(c: Int) = c and 255
    private fun near(a: Int, b: Int) = abs(red(a) - red(b)) + abs(green(a) - green(b)) + abs(blue(a) - blue(b)) <= TOLERANCE
    private fun luminance(c: Int) = (0.2126 * red(c) + 0.7152 * green(c) + 0.0722 * blue(c)) / 255.0

    /** [pixels] is ARGB row-major, size = width * height. Only near-white or near-black uniform margins are treated as removable. */
    fun analyze(pixels: IntArray, width: Int, height: Int): Analysis {
        require(width > 0 && height > 0 && pixels.size >= width * height)
        val whole = Box(0, 0, width, height)
        fun px(x: Int, y: Int) = pixels[y * width + x]
        // Average colour of the outer ring: used as the discreet backdrop for covers that must be fitted.
        var r = 0L; var g = 0L; var b = 0L; var n = 0L
        fun ring(x: Int, y: Int) { val c = px(x, y); r += red(c); g += green(c); b += blue(c); n++ }
        for (x in 0 until width) { ring(x, 0); ring(x, height - 1) }
        for (y in 1 until height - 1) { ring(0, y); ring(width - 1, y) }
        val edge = (0xff shl 24) or ((r / n).toInt() shl 16) or ((g / n).toInt() shl 8) or (b / n).toInt()
        val corner = px(0, 0)
        val corners = listOf(px(width - 1, 0), px(0, height - 1), px(width - 1, height - 1))
        val light = luminance(corner)
        val removable = (light >= .90 || light <= .10) && corners.all { near(it, corner) }
        if (!removable) return Analysis(width, height, whole, edge)
        fun rowEmpty(y: Int) = (0 until width).all { near(px(it, y), corner) }
        fun colEmpty(x: Int) = (0 until height).all { near(px(x, it), corner) }
        var top = 0; while (top < height && rowEmpty(top)) top++
        if (top == height) return Analysis(width, height, whole, edge) // blank page: keep it whole
        var bottom = height; while (bottom > top && rowEmpty(bottom - 1)) bottom--
        var left = 0; while (left < width && colEmpty(left)) left++
        var right = width; while (right > left && colEmpty(right - 1)) right--
        val keepX = (width * EDGE_KEEP).roundToInt(); val keepY = (height * EDGE_KEEP).roundToInt()
        left = (left - keepX).coerceAtLeast(0); right = (right + keepX).coerceAtMost(width)
        top = (top - keepY).coerceAtLeast(0); bottom = (bottom + keepY).coerceAtMost(height)
        // Safety: a sliver of content must not zoom the cover indiscriminately.
        if (left > width * MAX_MARGIN_REMOVAL) left = (width * MAX_MARGIN_REMOVAL).toInt()
        if (right < width * (1 - MAX_MARGIN_REMOVAL)) right = (width * (1 - MAX_MARGIN_REMOVAL)).toInt()
        if (top > height * MAX_MARGIN_REMOVAL) top = (height * MAX_MARGIN_REMOVAL).toInt()
        if (bottom < height * (1 - MAX_MARGIN_REMOVAL)) bottom = (height * (1 - MAX_MARGIN_REMOVAL)).toInt()
        return Analysis(width, height, Box(left, top, right, bottom), edge)
    }

    /**
     * [cardRatio] = width / height of the uniform card.
     * 1. Margin-absorbing fill: the smallest card-shaped window that contains the whole content area. If it fits inside the
     *    thumbnail, the card is filled with no empty space and nothing relevant is cut (text near the borders stays visible).
     * 2. Otherwise, a crop of at most [MAX_CUT] of the cover.
     * 3. Otherwise the whole cover is fitted (never distorted) over a backdrop derived from the cover itself.
     */
    fun plan(a: Analysis, cardRatio: Float): Plan {
        val c = cardRatio.coerceIn(.2f, 5f).toDouble()
        val content = a.content
        val needW = max(content.width.toDouble(), content.height * c)
        val needH = needW / c
        if (needW <= a.width + 0.5 && needH <= a.height + 0.5) {
            val w = needW.coerceAtMost(a.width.toDouble()); val h = w / c
            val cx = (content.left + content.right) / 2.0; val cy = (content.top + content.bottom) / 2.0
            val left = (cx - w / 2).coerceIn(0.0, a.width - w)
            val top = (cy - h / 2).coerceIn(0.0, (a.height - h).coerceAtLeast(0.0))
            return Plan(true, Box(left.roundToInt(), top.roundToInt(), (left + w).roundToInt().coerceAtMost(a.width), (top + h).roundToInt().coerceAtMost(a.height)))
        }
        val ratio = a.width.toDouble() / a.height
        val cut = 1 - min(ratio / c, c / ratio)
        if (cut <= MAX_CUT) {
            // Titles are usually near the top of tall covers; wide covers are centred.
            return if (ratio < c) { val h = a.width / c; Plan(true, Box(0, 0, a.width, min(a.height, h.roundToInt()))) }
            else { val w = a.height * c; val left = ((a.width - w) / 2).roundToInt(); Plan(true, Box(left, 0, min(a.width, left + w.roundToInt()), a.height)) }
        }
        return Plan(false, Box(0, 0, a.width, a.height))
    }
}
