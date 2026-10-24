package br.com.lottan.nativeapp.domain

import kotlin.math.*

object ThumbnailRules {
    const val MAX_EDGE = 384
    const val MAX_PIXELS = 196_608

    /** Preserve the complete page; bound both dimensions and total allocation. */
    fun size(width: Int, height: Int): Pair<Int, Int> {
        require(width > 0 && height > 0)
        val scale =
            minOf(
                MAX_EDGE.toDouble() / max(width, height),
                sqrt(MAX_PIXELS.toDouble() / (width.toDouble() * height)),
            )
        return max(1, floor(width * scale).toInt()) to max(1, floor(height * scale).toInt())
    }

    fun key(id: String, length: Long, modified: Long) = "$id-$length-$modified-v1"
}
