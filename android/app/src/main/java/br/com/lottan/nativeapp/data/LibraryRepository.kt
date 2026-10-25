package br.com.lottan.nativeapp.data

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.room.withTransaction
import br.com.lottan.nativeapp.domain.ReadingRules
import java.io.*
import java.time.*
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.security.MessageDigest

class DuplicatePdfException(val existingId: String) : IllegalArgumentException("Este PDF já está na biblioteca.")

class LibraryRepository(private val context: Context, private val db: LottanDatabase) {
    val dao = db.library()
    val books = dao.books()
    val groups = dao.groups()
    val days = dao.days()
    private val folder = File(context.filesDir, "pdfs").apply { mkdirs() }

    fun file(id: String): File {
        require(id.matches(Regex("[a-f0-9-]{36}")))
        return File(folder, "$id.pdf")
    }

    suspend fun import(uri: Uri, allowDuplicate: Boolean = false): Book =
        withContext(Dispatchers.IO) {
            val id = UUID.randomUUID().toString()
            val target = file(id)
            val partial = File(folder, "$id.part")
            try {
                val name =
                    context.contentResolver
                        .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                        ?.use { if (it.moveToFirst()) it.getString(0) else null } ?: "Documento.pdf"
                var bytes = 0L
                context.contentResolver.openInputStream(uri)?.use { input ->
                    partial.outputStream().use { out ->
                        val buffer = ByteArray(65536)
                        while (true) {
                            ensureActive()
                            val n = input.read(buffer)
                            if (n < 0) break
                            bytes += n
                            require(bytes <= ReadingRules.MAX_PDF_BYTES) {
                                "O PDF deve ter até 200 MB."
                            }
                            out.write(buffer, 0, n)
                        }
                    }
                } ?: error("Não foi possível acessar o arquivo.")
                if (!allowDuplicate) {
                    fun hash(file: File): ByteArray {
                        val digest = MessageDigest.getInstance("SHA-256")
                        file.inputStream().use { input -> val buffer = ByteArray(65536); while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) } }
                        return digest.digest()
                    }
                    val candidates = books.first().filter { it.bytes == bytes && file(it.id).exists() }
                    if (candidates.isNotEmpty()) {
                        val digest = hash(partial)
                        candidates.firstOrNull { hash(file(it.id)).contentEquals(digest) }?.let { throw DuplicatePdfException(it.id) }
                    }
                }
                PdfValidation.check(context, partial)
                val pages =
                    ParcelFileDescriptor.open(partial, ParcelFileDescriptor.MODE_READ_ONLY).use { fd
                        ->
                        PdfRenderer(fd).use {
                            require(it.pageCount > 0)
                            it.pageCount
                        }
                    }
                check(partial.renameTo(target)) { "Não foi possível salvar o PDF." }
                val book = Book(id, name, pages, bytes)
                dao.put(book)
                book
            } catch (e: Throwable) {
                partial.delete()
                target.delete()
                throw e
            }
        }

    suspend fun cover(group: String, uri: Uri, position: Float) = withContext(Dispatchers.IO) {
        require(group.matches(Regex("[a-f0-9-]{36}")))
        val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, options) }
        require(options.outWidth > 0 && options.outHeight > 0) { "Imagem inválida." }
        var sample = 1
        while (options.outWidth / sample > 1600 || options.outHeight / sample > 1600) sample *= 2
        options.inJustDecodeBounds = false; options.inSampleSize = sample
        val bitmap = context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, options) } ?: error("Imagem indisponível.")
        val target = android.graphics.Bitmap.createBitmap(640, 320, android.graphics.Bitmap.Config.ARGB_8888)
        val scale = maxOf(640f / bitmap.width, 320f / bitmap.height)
        val width = bitmap.width * scale; val height = bitmap.height * scale
        val x = (640 - width) * position.coerceIn(0f, 1f); val y = (320 - height) * position.coerceIn(0f, 1f)
        android.graphics.Canvas(target).drawBitmap(bitmap, null, android.graphics.RectF(x, y, x + width, y + height), android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
        val dir = File(context.filesDir, "folder-covers").apply { mkdirs() }
        val partial = File(dir, "$group.part")
        try { partial.outputStream().use { check(target.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it)) }; check(partial.renameTo(File(dir, "$group.jpg"))) }
        finally { bitmap.recycle(); target.recycle(); partial.delete() }
    }

    suspend fun remove(id: String) =
        withContext(Dispatchers.IO) {
            val f = file(id)
            check(!f.exists() || f.delete()) { "Não foi possível remover o PDF." }
            dao.removeBookMemberships(id)
            dao.removeReadPages(id)
            dao.delete(id)
        }

    suspend fun group(name: String, kind: String) {
        require(name.trim().isNotEmpty())
        dao.put(Group(UUID.randomUUID().toString(), name.trim().take(60), kind))
    }

    suspend fun removeGroup(id: String) =
        db.withTransaction {
            dao.removeMemberships(id)
            dao.clearFolder(id)
            dao.clearCategory(id)
            dao.deleteGroup(id)
        }

    suspend fun assign(id: String, kind: String, value: String) {
        if (kind == "folders") dao.folder(id, value) else dao.category(id, value)
    }

    suspend fun position(id: String, page: Int) = db.withTransaction {
        val book = dao.book(id) ?: return@withTransaction
        dao.progress(id, ReadingRules.page(page, book.pages), book.completed)
    }

    suspend fun visit(id: String, page: Int, sessionId: String = "", sessionStarted: Long = System.currentTimeMillis()) =
        db.withTransaction {
            val book = dao.book(id) ?: return@withTransaction
            val actual = ReadingRules.page(page, book.pages)
            dao.readPage(ReadPage(id, actual))
            dao.completion(id, dao.readCount(id) >= book.pages)
            if (!PreferenceRepository(context).state.first().recordActivity) return@withTransaction
            val key = LocalDate.now().toString()
            val counted = dao.countedToday(key, id, actual)
            val event = dao.event(ReadingPageEvent(sessionId.ifEmpty { "legacy:$key" }, id, actual, key))
            if (!counted) {
                val day = dao.day(key) ?: Day(key)
                dao.put(day.copy(visits = day.visits + 1))
            }
            if (sessionId.isNotEmpty() && event != -1L) {
                dao.put(ReadingSession(sessionId, sessionStarted))
                dao.sessionActivity(sessionId, 0, 1)
            }
        }

    suspend fun restart(id: String) =
        db.withTransaction {
            val book = dao.book(id) ?: error("PDF não encontrado.")
            // Retains completed flag, visits and historical reading time.
            dao.progress(id, 0, book.completed)
        }

    suspend fun record(start: Instant, end: Instant, sessionId: String = "", sessionStarted: Long = start.toEpochMilli()) =
        db.withTransaction {
            if (!PreferenceRepository(context).state.first().recordActivity) return@withTransaction
            if (sessionId.isNotEmpty() && end > start) {
                dao.put(ReadingSession(sessionId, sessionStarted)); dao.sessionActivity(sessionId, java.time.Duration.between(start, end).toMillis(), 0)
            }
            ReadingRules.split(start, end, ZoneId.systemDefault()).forEach { (date, ms) ->
                val old = dao.day(date) ?: Day(date)
                dao.put(old.copy(millis = old.millis + ms))
            }
        }
}
