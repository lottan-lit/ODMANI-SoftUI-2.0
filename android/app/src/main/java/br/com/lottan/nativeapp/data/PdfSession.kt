package br.com.lottan.nativeapp.data

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** PdfRenderer is serialized. Only requested pages are rendered; cache is bounded to 24 MiB. */
class PdfSession(private val file: File) {
    private val mutex = Mutex()
    private var fd: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private val cache =
        object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
            override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
        }

    suspend fun render(index: Int, width: Int): Bitmap =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                if (renderer == null) {
                    fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    try {
                        renderer = PdfRenderer(fd!!)
                    } catch (e: Exception) {
                        fd?.close()
                        fd = null
                        throw e
                    }
                }
                val bounded = width.coerceIn(360, 1800)
                val key = "$index:$bounded"
                cache.get(key)
                    ?: renderer!!.openPage(index).use { page ->
                        val scale =
                            minOf(
                                bounded.toDouble() / page.width,
                                kotlin.math.sqrt(
                                    4_000_000.0 / (page.width.toDouble() * page.height)
                                ),
                            )
                        val bitmap =
                            Bitmap.createBitmap(
                                (page.width * scale).toInt().coerceAtLeast(1),
                                (page.height * scale).toInt().coerceAtLeast(1),
                                Bitmap.Config.ARGB_8888,
                            )
                        try {
                            bitmap.eraseColor(Color.WHITE)
                            page.render(
                                bitmap,
                                null,
                                null,
                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
                            )
                            cache.put(key, bitmap)
                            bitmap
                        } catch (e: Throwable) {
                            bitmap.recycle()
                            throw e
                        }
                    }
            }
        }

    suspend fun close() =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                cache.evictAll()
                renderer?.close()
                renderer = null
                fd?.close()
                fd = null
            }
        }
}
