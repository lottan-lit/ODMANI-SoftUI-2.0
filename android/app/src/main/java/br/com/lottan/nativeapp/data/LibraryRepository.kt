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

    suspend fun import(uri: Uri): Book =
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

    suspend fun remove(id: String) =
        withContext(Dispatchers.IO) {
            val f = file(id)
            check(!f.exists() || f.delete()) { "Não foi possível remover o PDF." }
            dao.delete(id)
        }

    suspend fun group(name: String, kind: String) {
        require(name.trim().isNotEmpty())
        dao.put(Group(UUID.randomUUID().toString(), name.trim().take(60), kind))
    }

    suspend fun removeGroup(id: String) =
        db.withTransaction {
            dao.clearFolder(id)
            dao.clearCategory(id)
            dao.deleteGroup(id)
        }

    suspend fun assign(id: String, kind: String, value: String) {
        if (kind == "folders") dao.folder(id, value) else dao.category(id, value)
    }

    suspend fun visit(id: String, page: Int) =
        db.withTransaction {
            val book = dao.book(id) ?: return@withTransaction
            val actual = ReadingRules.page(page, book.pages)
            dao.progress(id, actual, actual == book.pages - 1)
            val key = LocalDate.now().toString()
            val day = dao.day(key) ?: Day(key)
            dao.put(day.copy(visits = day.visits + 1))
        }

    suspend fun restart(id: String) =
        db.withTransaction {
            val book = dao.book(id) ?: error("PDF não encontrado.")
            // Retains completed flag, visits and historical reading time.
            dao.progress(id, 0, book.completed)
        }

    suspend fun record(start: Instant, end: Instant) =
        db.withTransaction {
            ReadingRules.split(start, end, ZoneId.systemDefault()).forEach { (date, ms) ->
                val old = dao.day(date) ?: Day(date)
                dao.put(old.copy(millis = old.millis + ms))
            }
        }
}
