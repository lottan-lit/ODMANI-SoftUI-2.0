package br.com.lottan.nativeapp

import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.lottan.nativeapp.data.*
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThumbnailRepositoryTest {
    @Test
    fun originalColorsAspectCacheAndInvalidation() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, LottanDatabase::class.java).build()
        val library = LibraryRepository(context, db)
        val id = UUID.randomUUID().toString()
        val source = library.file(id)
        val book = Book(id, "Capa.pdf", 1, 0)
        fun write(left: Int) {
            val pdf = PdfDocument()
            try {
                val page = pdf.startPage(PdfDocument.PageInfo.Builder(300, 200, 1).create())
                page.canvas.drawRect(0f, 0f, 150f, 200f, Paint().apply { color = left })
                page.canvas.drawRect(150f, 0f, 300f, 200f, Paint().apply { color = Color.BLUE })
                pdf.finishPage(page)
                source.outputStream().use(pdf::writeTo)
            } finally {
                pdf.close()
            }
        }
        try {
            write(Color.RED)
            val repo = ThumbnailRepository(context, library)
            val cover = repo.load(book)
            assertEquals(1.5, cover.width.toDouble() / cover.height, .01)
            assertEquals(Color.RED, cover.getPixel(cover.width / 4, cover.height / 2))
            assertEquals(Color.BLUE, cover.getPixel(cover.width * 3 / 4, cover.height / 2))
            assertSame(cover, repo.load(book))
            val fromDisk = ThumbnailRepository(context, library).load(book)
            assertEquals(cover.getPixel(10, 10), fromDisk.getPixel(10, 10))
            val old = source.lastModified()
            write(Color.GREEN)
            source.setLastModified(old + 2000)
            val changed = repo.load(book)
            assertEquals(Color.GREEN, changed.getPixel(changed.width / 4, changed.height / 2))
        } finally {
            source.delete()
            db.close()
        }
    }
}
