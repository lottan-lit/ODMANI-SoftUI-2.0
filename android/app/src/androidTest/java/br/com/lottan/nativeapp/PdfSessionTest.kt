package br.com.lottan.nativeapp

import android.graphics.pdf.PdfDocument
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.lottan.nativeapp.data.PdfSession
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PdfSessionTest {
    @Test
    fun rendererBoundsAndInvalidFiles() = runBlocking {
        val cache = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        val valid = File.createTempFile("lottan-valid-", ".pdf", cache)
        val invalid = File.createTempFile("lottan-invalid-", ".pdf", cache)
        val session = PdfSession(valid)
        val broken = PdfSession(invalid)
        try {
            val document = PdfDocument()
            try {
                val page = document.startPage(PdfDocument.PageInfo.Builder(600, 800, 1).create())
                document.finishPage(page)
                valid.outputStream().use(document::writeTo)
            } finally {
                document.close()
            }
            val bitmap = session.render(0, 10000)
            assertTrue(bitmap.width <= 1800)
            assertTrue(bitmap.width.toLong() * bitmap.height <= 4_000_000)
            assertSame(bitmap, session.render(0, 10000))
            invalid.writeText("not a PDF")
            var rejected = false
            try {
                broken.render(0, 600)
            } catch (_: Exception) {
                rejected = true
            }
            assertTrue(rejected)
        } finally {
            session.close()
            broken.close()
            valid.delete()
            invalid.delete()
        }
    }
}
