package br.com.lottan.nativeapp.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import br.com.lottan.nativeapp.domain.ThumbnailRules
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Original first-page pixels only. No titles, overlays, crops, tint or theme-dependent filters. */
class ThumbnailRepository(context: Context, private val library: LibraryRepository) {
    private val directory = File(context.cacheDir, "pdf-covers").apply { mkdirs() }
    private val gate = Mutex()
    private val memory =
        object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
            override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
        }

    suspend fun load(book: Book): Bitmap =
        withContext(Dispatchers.IO) {
            gate.withLock {
                val source = library.file(book.id)
                check(source.isFile) { "PDF não encontrado." }
                val key = ThumbnailRules.key(book.id, source.length(), source.lastModified())
                memory.get(key)?.let {
                    return@withLock it
                }
                val disk = File(directory, "$key.png")
                if (disk.isFile) {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(disk.path, bounds)
                    if (
                        bounds.outWidth in 1..ThumbnailRules.MAX_EDGE &&
                            bounds.outHeight in 1..ThumbnailRules.MAX_EDGE
                    ) {
                        val cached = BitmapFactory.decodeFile(disk.path)
                        if (cached != null) {
                            memory.put(key, cached)
                            return@withLock cached
                        }
                    }
                    disk.delete()
                }
                val bitmap =
                    ParcelFileDescriptor.open(source, ParcelFileDescriptor.MODE_READ_ONLY).use { fd
                        ->
                        PdfRenderer(fd).use { renderer ->
                            check(renderer.pageCount > 0)
                            renderer.openPage(0).use { page ->
                                val (width, height) = ThumbnailRules.size(page.width, page.height)
                                val image =
                                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                try {
                                    image.eraseColor(Color.WHITE)
                                    page.render(
                                        image,
                                        null,
                                        null,
                                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
                                    )
                                    image
                                } catch (e: Throwable) {
                                    image.recycle()
                                    throw e
                                }
                            }
                        }
                    }
                ensureActive()
                // A replaced file must not seed a stale cache entry.
                if (key == ThumbnailRules.key(book.id, source.length(), source.lastModified())) {
                    memory.put(key, bitmap)
                    val temporary = File(directory, "$key.part")
                    try {
                        temporary.outputStream().use {
                            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
                        }
                        if (!temporary.renameTo(disk)) temporary.delete()
                    } catch (_: Exception) {
                        temporary.delete()
                    }
                    directory
                        .listFiles()
                        ?.filter { it.name.startsWith("${book.id}-") && it.name != disk.name }
                        ?.forEach { it.delete() }
                    trimDisk()
                }
                bitmap
            }
        }

    suspend fun evict(id: String) =
        withContext(Dispatchers.IO) {
            gate.withLock {
                memory
                    .snapshot()
                    .keys
                    .filter { it.startsWith("$id-") }
                    .forEach { memory.remove(it) }
                directory
                    .listFiles()
                    ?.filter { it.name.startsWith("$id-") }
                    ?.forEach { it.delete() }
            }
        }

    private fun trimDisk() {
        val files =
            directory.listFiles()?.filter { it.extension == "png" }?.sortedBy { it.lastModified() }
                ?: return
        var bytes = files.sumOf { it.length() }
        for (file in files) {
            if (bytes <= 32L * 1024 * 1024) break
            val size = file.length()
            if (file.delete()) bytes -= size
        }
    }
}
