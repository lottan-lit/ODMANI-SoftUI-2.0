package br.com.lottan.nativeapp
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import br.com.lottan.nativeapp.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
class ActivityImportRegressionTest {
    @Test fun importingAndDuplicateDetectionDoNotCreateActivity() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, LottanDatabase::class.java).build()
        val source = File(context.cacheDir, "regression-source.pdf")
        val repository = LibraryRepository(context, db)
        var id: String? = null
        try {
            PdfDocument().let { doc -> val page = doc.startPage(PdfDocument.PageInfo.Builder(100, 200, 1).create()); doc.finishPage(page); source.outputStream().use { doc.writeTo(it) }; doc.close() }
            val book = repository.import(Uri.fromFile(source)); id = book.id
            assertTrue(db.library().days().first().isEmpty())
            assertEquals(0L, book.lastOpened)
            try { repository.import(Uri.fromFile(source)); fail("Duplicate accepted without confirmation") } catch (expected: DuplicatePdfException) { assertEquals(book.id, expected.existingId) }
            assertEquals(1, db.library().books().first().size)
            assertTrue(db.library().days().first().isEmpty())
            repository.visit(book.id, 0, "test-session", 123L)
            repository.visit(book.id, 0, "test-session", 123L)
            assertEquals(1, db.library().readCount(book.id))
            assertEquals(1L, db.library().days().first().single().visits)
            repository.record(java.time.Instant.ofEpochMilli(123), java.time.Instant.ofEpochMilli(60123), "test-session", 123)
            val session = db.library().sessions().first().single()
            assertEquals(1L, session.visits)
            assertEquals(60000L, session.millis)
        } finally { id?.let { repository.file(it).delete() }; source.delete(); db.close() }
    }
    @Test fun progressIsImmediateAndDailyPagesAreUniqueAcrossSessions() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, LottanDatabase::class.java).build()
        try {
            val repository = LibraryRepository(context, db)
            db.library().put(Book("progress", "progress.pdf", 3, 12))
            repository.position("progress", 1)
            assertEquals(1, db.library().book("progress")!!.page)
            assertEquals(0, db.library().readCount("progress"))
            assertTrue(db.library().days().first().isEmpty())
            repository.visit("progress", 1, "a", 100)
            repository.visit("progress", 1, "a", 100)
            repository.visit("progress", 1, "b", 200)
            repository.visit("progress", 2, "b", 200)
            assertEquals(2L, db.library().days().first().single().visits)
            assertEquals(2, db.library().readCount("progress"))
            assertEquals(listOf(1L, 2L), db.library().sessions().first().map { it.visits })
            assertFalse(db.library().book("progress")!!.completed)
            repository.visit("progress", 0, "b", 200)
            assertTrue(db.library().book("progress")!!.completed)
        } finally { db.close() }
    }
    @Test fun oneDocumentCanBelongToSeveralFolders() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, LottanDatabase::class.java).build()
        val repository = LibraryRepository(context, db)
        try {
            val dao = db.library()
            dao.put(Book("doc", "test", 3, 12))
            dao.put(Group("a", "A", "folders")); dao.put(Group("b", "B", "folders"))
            dao.addFolder(FolderBook("a", "doc")); dao.addFolder(FolderBook("b", "doc"))
            assertEquals(2, dao.memberships().first().size)
            repository.removeGroup("a")
            assertNotNull(dao.book("doc"))
            assertEquals(listOf(FolderBook("b", "doc")), dao.memberships().first())
        } finally { db.close() }
    }
    @Test fun invalidAndProtectedImportsDoNotPoisonValidPdf() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val db = Room.inMemoryDatabaseBuilder(context, LottanDatabase::class.java).build()
        val repository = LibraryRepository(context, db)
        val invalid = File(context.cacheDir, "invalid-regression.pdf").apply { writeText("%PDF-1.4\nbroken") }
        val protected = File(context.cacheDir, "protected-regression.pdf")
        instrumentation.context.assets.open("protected.pdf").use { input -> protected.outputStream().use { input.copyTo(it) } }
        val valid = File(context.cacheDir, "valid-after-errors.pdf")
        var imported: String? = null
        try {
            for (source in listOf(invalid, protected)) {
                var rejected = false
                try { repository.import(Uri.fromFile(source)) } catch (error: Exception) { rejected = true }
                assertTrue("Invalid/protected PDF must be rejected", rejected)
            }
            PdfDocument().let { doc -> val page = doc.startPage(PdfDocument.PageInfo.Builder(100, 200, 1).create()); doc.finishPage(page); valid.outputStream().use { doc.writeTo(it) }; doc.close() }
            val book = repository.import(Uri.fromFile(valid)); imported = book.id
            assertEquals(1, book.pages)
            val renderer = PdfSession(repository.file(book.id), context)
            try { assertTrue(renderer.render(0, 400).width > 0) } finally { renderer.close() }
        } finally { imported?.let { repository.file(it).delete() }; invalid.delete(); protected.delete(); valid.delete(); db.close() }
    }
}
