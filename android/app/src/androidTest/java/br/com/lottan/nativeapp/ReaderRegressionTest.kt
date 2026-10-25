package br.com.lottan.nativeapp
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.File
class ReaderRegressionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun tappingPdfOnlyTogglesControls() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = context.applicationContext as LottanApplication
        app.preferences.hide(true)
        app.preferences.direction("single")
        val source = File(context.cacheDir, "reader-ui-regression.pdf")
        PdfDocument().let { doc -> repeat(2) { i -> val page = doc.startPage(PdfDocument.PageInfo.Builder(200, 300, i + 1).create()); doc.finishPage(page) }; source.outputStream().use { doc.writeTo(it) }; doc.close() }
        val book = app.library.import(Uri.fromFile(source))
        app.library.dao.put(book.copy(name = "reader-ui-regression.pdf"))
        try {
            compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Biblioteca").fetchSemanticsNodes().isNotEmpty() || compose.onAllNodesWithText("Começar", substring = true).fetchSemanticsNodes().isNotEmpty() }
            if (compose.onAllNodesWithText("Começar", substring = true).fetchSemanticsNodes().isNotEmpty()) compose.onNodeWithText("Começar", substring = true).performClick()
            compose.onNodeWithContentDescription("Biblioteca").performClick()
            compose.onNodeWithTag("document-grid").performScrollToNode(hasText("reader-ui-regression"))
            compose.onNodeWithText("reader-ui-regression").performClick()
            compose.onNodeWithText("Ler", substring = false).performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Página 1").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Página 1").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("Voltar").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithContentDescription("Voltar").assertDoesNotExist()
            compose.onNodeWithContentDescription("Página 1").assertExists().performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithContentDescription("Voltar").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Voltar").assertExists()
            compose.onNodeWithText("›").performClick()
            compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Página 2").fetchSemanticsNodes().isNotEmpty() }
        } finally { app.library.remove(book.id); source.delete() }
    }
    @Test fun quickExitPersistsPositionAndRecreationKeepsZoom() = runBlocking<Unit> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = context.applicationContext as LottanApplication
        app.preferences.hide(true)
        app.preferences.direction("single")
        val source = File(context.cacheDir, "reader-state-regression.pdf")
        PdfDocument().let { doc ->
            repeat(3) { i -> val page = doc.startPage(PdfDocument.PageInfo.Builder(200, 300, i + 1).create()); doc.finishPage(page) }
            source.outputStream().use { doc.writeTo(it) }; doc.close()
        }
        val book = app.library.import(Uri.fromFile(source))
        app.library.dao.put(book.copy(name = "reader-state-regression.pdf"))
        try {
            compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("Biblioteca").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Biblioteca").performClick()
            compose.onNodeWithTag("document-grid").performScrollToNode(hasText("reader-state-regression"))
            compose.onNodeWithText("reader-state-regression").performClick()
            compose.onNodeWithText("Ler", substring = false).performClick()
            compose.waitUntil(10000) { compose.onAllNodesWithText("1 / 3").fetchSemanticsNodes().isNotEmpty() }
            val start = android.os.SystemClock.elapsedRealtime()
            compose.onNodeWithText("›").performClick()
            compose.waitUntil(3000) { compose.onAllNodesWithText("2 / 3").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Voltar").performClick()
            org.junit.Assert.assertTrue("Exit must precede page qualification", android.os.SystemClock.elapsedRealtime() - start < 5000)
            compose.waitUntil(3000) { runBlocking { app.library.dao.book(book.id)?.page == 1 } }
            compose.onNodeWithText("Continuar", substring = false).performClick()
            compose.waitUntil(10000) { compose.onAllNodesWithText("2 / 3").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("+").performClick()
            compose.onNodeWithText("125%").assertExists()
            compose.activityRule.scenario.recreate()
            compose.waitUntil(10000) { compose.onAllNodesWithText("125%").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("2 / 3").assertExists()
            kotlinx.coroutines.delay(6200)
            org.junit.Assert.assertEquals(1, app.library.dao.readCount(book.id))
            compose.onNodeWithContentDescription("Voltar").performClick()
        } finally { app.library.remove(book.id); source.delete() }
    }
}
