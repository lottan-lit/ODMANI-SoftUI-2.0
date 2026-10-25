package br.com.lottan.nativeapp

import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import br.com.lottan.nativeapp.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

class Library21Test {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as LottanApplication
    private suspend fun document(name: String, official: Boolean = false): Book {
        val file = File(app.cacheDir, "b21-${UUID.randomUUID()}.pdf")
        PdfDocument().let { pdf ->
            repeat(3) { i -> val p = pdf.startPage(PdfDocument.PageInfo.Builder(300, 420, i + 1).create()); p.canvas.drawText(name, 15f, 35f, android.graphics.Paint()); pdf.finishPage(p) }
            file.outputStream().use { pdf.writeTo(it) }; pdf.close()
        }
        val book = app.library.import(Uri.fromFile(file), true).copy(name = "$name.pdf", origin = if (official) "lottan_verified" else "personal")
        app.library.dao.put(book); file.delete(); return book
    }
    private fun library() {
        compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("Biblioteca").fetchSemanticsNodes().isNotEmpty() || compose.onAllNodesWithText("Começar", substring = true).fetchSemanticsNodes().isNotEmpty() }
        if (compose.onAllNodesWithText("Começar", substring = true).fetchSemanticsNodes().isNotEmpty()) compose.onNodeWithText("Começar", substring = true).performClick()
        compose.onNodeWithContentDescription("Biblioteca").performClick()
    }
    private fun row(book: Book) = compose.onNodeWithTag("library-book-${book.id}")
    private fun reveal(book: Book) { compose.onNodeWithTag("document-grid").performScrollToNode(hasTestTag("library-book-${book.id}")) }
    private fun top() { compose.onNodeWithTag("document-grid").performScrollToIndex(0) }
    private fun menu(book: Book, title: String) {
        reveal(book)
        compose.onNodeWithContentDescription("Opções de $title").performClick()
    }
    @Test fun modesSearchSortingSelectionAndActionsPreserveData() = runBlocking<Unit> {
        val before = app.preferences.state.first()
        val books = mutableListOf<Book>()
        val folder = Group(UUID.randomUUID().toString(), "Biblioteca21 pasta", "folders")
        val category = Group(UUID.randomUUID().toString(), "Biblioteca21 categoria", "categories")
        try {
            app.preferences.libraryView("grid"); app.preferences.sort("Recentes")
            books += document("Biblioteca21 A curto")
            books += document("Biblioteca21 B título extenso para testar o alinhamento das duas linhas")
            books += document("Biblioteca21 C editorial", true)
            app.library.dao.put(folder); app.library.dao.put(category)
            val a = books[0]; val b = books[1]
            library()
            compose.onNode(hasSetTextAction()).assertDoesNotExist()
            compose.onNodeWithContentDescription("Pesquisar documentos").performClick()
            compose.onNode(hasSetTextAction()).performTextInput("Biblioteca21")
            compose.onNodeWithText("3 documentos encontrados").assertExists()
            compose.onNode(hasSetTextAction()).performImeAction()
            compose.waitUntil(5000) { compose.onAllNodesWithTag("floating-navigation").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Exibir em lista").performClick()
            compose.waitUntil(5000) { compose.onAllNodesWithTag("library-list").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Ordenar documentos").performClick()
            compose.onNodeWithText("Título", substring = false).performClick()
            compose.onNodeWithText("Lottan", substring = false).performClick()
            compose.onNodeWithText("1 documento encontrado").assertExists()
            compose.onNodeWithText("Pessoais", substring = false).performClick()
            compose.onNodeWithText("2 documentos encontrados").assertExists()
            assertEquals("Título", app.preferences.state.first().librarySort)
            assertEquals("list", app.preferences.state.first().libraryView)
            compose.activityRule.scenario.recreate()
            compose.waitUntil(10000) { compose.onAllNodesWithTag("library-list").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("2 documentos encontrados").assertExists()
            menu(a, "Biblioteca21 A curto")
            compose.onNodeWithText("Renomear", substring = false).performClick()
            compose.onNode(hasSetTextAction().and(hasText("Novo título", substring = true))).performTextReplacement("Biblioteca21 A renomeado")
            compose.onNodeWithText("Salvar", substring = false).performClick()
            compose.waitUntil(5000) { runBlocking { app.library.dao.book(a.id)?.name == "Biblioteca21 A renomeado" } }
            reveal(a); row(a).performTouchInput { longClick() }
            top(); compose.onNodeWithText("1 selecionado").assertExists()
            reveal(b); row(b).performClick()
            top(); compose.onNodeWithText("2 selecionados").assertExists()
            compose.onNodeWithContentDescription("Exibir em grade").performClick()
            compose.waitUntil(5000) { compose.onAllNodesWithTag("library-grid").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("2 selecionados").assertExists()
            compose.onNodeWithText("Adicionar à pasta", substring = false).performClick()
            compose.onNodeWithText(folder.name, substring = false).performClick()
            compose.waitUntil(5000) { runBlocking { app.library.dao.memberships().first().count { it.folderId == folder.id } == 2 } }
            menu(a, "Biblioteca21 A renomeado")
            compose.onNodeWithText("Alterar categoria", substring = false).performClick()
            compose.onNodeWithText(category.name, substring = false).performClick()
            compose.waitUntil(5000) { runBlocking { app.library.dao.book(a.id)?.category == category.id } }
            menu(a, "Biblioteca21 A renomeado")
            compose.onNodeWithText("Remover documento", substring = false).performClick()
            compose.onNodeWithText("Cancelar", substring = false).performClick()
            assertTrue(app.library.file(a.id).exists()); assertTrue(app.library.file(b.id).exists())
            menu(a, "Biblioteca21 A renomeado")
            compose.onNodeWithText("Ver detalhes", substring = false).performClick()
            compose.onNodeWithText("Biblioteca21 A renomeado", substring = false).assertExists()
            compose.onNodeWithContentDescription("Voltar").performClick()
            top(); compose.onNodeWithContentDescription("Fechar pesquisa").performClick()
            compose.onNode(hasSetTextAction()).assertDoesNotExist()
            assertEquals("Título", app.preferences.state.first().librarySort)
        } finally {
            books.forEach { app.library.remove(it.id) }
            app.library.removeGroup(folder.id); app.library.removeGroup(category.id)
            app.preferences.libraryView(before.libraryView); app.preferences.sort(before.librarySort)
        }
    }
}
