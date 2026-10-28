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

/** Name rules shared by category creation and renaming. Accents are preserved; comparison ignores case only. */
object CategoryRules {
    const val MAX_LENGTH = 40
    val suggestions = listOf("Estudos", "Finanças", "Literatura", "Psicologia", "Filosofia", "Trabalho", "Administração", "Documentos", "Faculdade", "Projetos")
    fun clean(name: String) = name.trim().replace(Regex("\\s+"), " ")
    fun validate(name: String, existing: List<String>): String {
        val value = clean(name)
        require(value.isNotEmpty()) { "Informe um nome para a categoria." }
        require(value.length <= MAX_LENGTH && value.none { it.code < 32 }) { "Use até $MAX_LENGTH caracteres, sem quebras de linha." }
        require(existing.none { it.equals(value, ignoreCase = true) }) { "Já existe uma categoria com esse nome." }
        return value
    }
}

class DuplicatePdfException(val existingId: String) : IllegalArgumentException("Este PDF já está na biblioteca.")

data class PreparedPdf(val id: String, val name: String, val pages: Int, val bytes: Long)

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

    suspend fun prepareImport(uri: Uri, allowDuplicate: Boolean = false): PreparedPdf {
        val id = UUID.randomUUID().toString()
        try { return prepareImportFile(uri, allowDuplicate, id) }
        catch (e: Throwable) {
            withContext(Dispatchers.IO + NonCancellable) { File(folder, "$id.part").delete() }
            throw e
        }
    }

    private suspend fun prepareImportFile(uri: Uri, allowDuplicate: Boolean, id: String): PreparedPdf =
        withContext(Dispatchers.IO) {
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
                PreparedPdf(id, name, pages, bytes)
            } catch (e: Throwable) {
                partial.delete()
                throw e
            }
        }

    suspend fun import(uri: Uri, allowDuplicate: Boolean = false): Book = finishImport(prepareImport(uri, allowDuplicate), emptySet())

    suspend fun discardImport(value: PreparedPdf) = withContext(Dispatchers.IO) { File(folder, "${value.id}.part").delete(); Unit }
    suspend fun cleanupStaging() = withContext(Dispatchers.IO) {
        val committed = dao.books().first().map { "${it.id}.pdf" }.toSet()
        folder.listFiles()?.filter { it.name.endsWith(".part") || it.isFile && it.name.endsWith(".pdf") && it.name !in committed }?.forEach { it.delete() }
        Unit
    }

    suspend fun finishImport(value: PreparedPdf, folderIds: Set<String>): Book = withContext(Dispatchers.IO + NonCancellable) {
        require(value.id.matches(Regex("[a-f0-9-]{36}")))
        val partial = File(folder, "${value.id}.part")
        val target = file(value.id)
        var moved = false
        try {
            db.withTransaction {
                dao.book(value.id)?.let { return@withTransaction it }
                val existing = dao.groups().first().filter { it.kind == "folders" }.map { it.id }.toSet()
                require(existing.containsAll(folderIds)) { "Uma pasta selecionada não está mais disponível." }
                check(partial.isFile && partial.length() == value.bytes) { "A seleção expirou. Selecione o PDF novamente." }
                check(!target.exists() && partial.renameTo(target)) { "Não foi possível salvar o PDF. Verifique o armazenamento." }
                moved = true
                val book = Book(value.id, value.name, value.pages, value.bytes)
                dao.put(book)
                folderIds.forEach { dao.addFolder(FolderBook(it, book.id)) }
                book
            }
        } catch (e: Throwable) { partial.delete(); if (moved) target.delete(); throw e }
    }

    suspend fun setFolders(bookId: String, folderIds: Set<String>) = db.withTransaction {
        require(dao.book(bookId) != null) { "PDF não encontrado." }
        val existing = dao.groups().first().filter { it.kind == "folders" }.map { it.id }.toSet()
        require(existing.containsAll(folderIds)) { "Pasta não encontrada." }
        dao.removeBookMemberships(bookId)
        folderIds.forEach { dao.addFolder(FolderBook(it, bookId)) }
    }

    suspend fun setFolderMembers(folderId: String, bookIds: Set<String>) = db.withTransaction {
        require(dao.groups().first().any { it.id == folderId && it.kind == "folders" }) { "Pasta não encontrada." }
        val existing = dao.books().first().map { it.id }.toSet()
        require(existing.containsAll(bookIds)) { "Um PDF não está mais disponível." }
        dao.removeMemberships(folderId)
        bookIds.forEach { dao.addFolder(FolderBook(folderId, it)) }
    }

    suspend fun saveFolder(id: String?, name: String, color: String, icon: String): Group = db.withTransaction {
        val clean = name.trim()
        require(clean.isNotEmpty() && clean.length <= 60 && clean.none { it.code < 32 }) { "Informe um nome de 1 a 60 caracteres, sem quebras de linha." }
        require(dao.groups().first().none { it.kind == "folders" && it.id != id && it.name.equals(clean, true) }) { "Já existe uma pasta com esse nome." }
        val value = Group(id ?: UUID.randomUUID().toString(), clean, "folders", color, icon)
        if (id == null) dao.put(value) else {
            require(dao.groups().first().any { it.id == id && it.kind == "folders" }) { "Pasta não encontrada." }
            dao.updateGroup(id, clean, color, icon)
        }
        value
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
            dao.removeBookCategories(id)
            dao.removeReadPages(id)
            dao.delete(id)
        }

    suspend fun group(name: String, kind: String) {
        require(name.trim().isNotEmpty())
        dao.put(Group(UUID.randomUUID().toString(), name.trim().take(60), kind))
    }

    /** Deleting a folder or category removes only the classification and its links, never the PDFs. */
    suspend fun removeGroup(id: String) =
        db.withTransaction {
            dao.removeMemberships(id)
            dao.removeCategoryLinks(id)
            dao.clearFolder(id)
            dao.clearCategory(id)
            dao.deleteGroup(id)
        }

    /** Single-value shim kept for older callers; categories are now many-to-many. */
    suspend fun assign(id: String, kind: String, value: String) {
        if (kind == "folders") setFolders(id, if (value.isBlank()) emptySet() else setOf(value))
        else setBookCategories(id, if (value.isBlank()) emptySet() else setOf(value))
    }

    // ---- Categories (many-to-many) ----

    suspend fun saveCategory(id: String?, name: String): Group = db.withTransaction {
        val clean = CategoryRules.validate(name, dao.groups().first().filter { it.kind == "categories" && it.id != id }.map { it.name })
        if (id == null) Group(UUID.randomUUID().toString(), clean, "categories").also { dao.put(it) }
        else {
            val current = dao.groups().first().firstOrNull { it.id == id && it.kind == "categories" } ?: error("Categoria não encontrada.")
            dao.renameGroup(id, clean)
            current.copy(name = clean)
        }
    }

    private suspend fun requireCategory(id: String) = require(dao.groups().first().any { it.id == id && it.kind == "categories" }) { "Categoria não encontrada." }
    private suspend fun requireFolder(id: String) = require(dao.groups().first().any { it.id == id && it.kind == "folders" }) { "Pasta não encontrada." }
    private suspend fun requireBooks(ids: Set<String>) = require(dao.books().first().map { it.id }.toSet().containsAll(ids)) { "Um PDF não está mais disponível." }

    suspend fun setBookCategories(bookId: String, categoryIds: Set<String>) = db.withTransaction {
        require(dao.book(bookId) != null) { "PDF não encontrado." }
        val valid = dao.groups().first().filter { it.kind == "categories" }.map { it.id }.toSet()
        require(valid.containsAll(categoryIds)) { "Categoria não encontrada." }
        dao.removeBookCategories(bookId)
        categoryIds.forEach { dao.addCategory(BookCategory(it, bookId)) }
    }

    /** Batch: link many PDFs to a category. Duplicate links are ignored; returns how many links were created. */
    suspend fun addBooksToCategory(categoryId: String, bookIds: Set<String>): Int = db.withTransaction {
        requireCategory(categoryId); requireBooks(bookIds)
        val before = dao.categoryLinksNow().count { it.categoryId == categoryId }
        bookIds.forEach { dao.addCategory(BookCategory(categoryId, it)) }
        dao.categoryLinksNow().count { it.categoryId == categoryId } - before
    }

    suspend fun removeBooksFromCategory(categoryId: String, bookIds: Set<String>) = db.withTransaction {
        requireCategory(categoryId); bookIds.forEach { dao.removeCategory(categoryId, it) }
    }

    suspend fun addBooksToFolder(folderId: String, bookIds: Set<String>): Int = db.withTransaction {
        requireFolder(folderId); requireBooks(bookIds)
        val before = dao.membershipsNow().count { it.folderId == folderId }
        bookIds.forEach { dao.addFolder(FolderBook(folderId, it)) }
        dao.membershipsNow().count { it.folderId == folderId } - before
    }

    suspend fun removeBooksFromFolder(folderId: String, bookIds: Set<String>) = db.withTransaction {
        requireFolder(folderId); bookIds.forEach { dao.removeFolder(folderId, it) }
    }

    suspend fun setCategoryMembers(categoryId: String, bookIds: Set<String>) = db.withTransaction {
        requireCategory(categoryId); requireBooks(bookIds)
        dao.removeCategoryLinks(categoryId)
        bookIds.forEach { dao.addCategory(BookCategory(categoryId, it)) }
    }

    suspend fun position(id: String, page: Int) = db.withTransaction {
        val book = dao.book(id) ?: return@withTransaction
        dao.progress(id, ReadingRules.page(page, book.pages), book.completed)
    }

    suspend fun visit(id: String, page: Int, sessionId: String = "", sessionStarted: Long = System.currentTimeMillis(), date: LocalDate = LocalDate.now()) =
        db.withTransaction {
            val book = dao.book(id) ?: return@withTransaction
            val actual = ReadingRules.page(page, book.pages)
            dao.readPage(ReadPage(id, actual))
            dao.completion(id, dao.readCount(id) >= book.pages)
            if (!PreferenceRepository(context).state.first().recordActivity) return@withTransaction
            val key = date.toString()
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
