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

class CorrectionsUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun enter() {
        compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("Biblioteca").fetchSemanticsNodes().isNotEmpty() || compose.onAllNodesWithText("Começar", substring = true).fetchSemanticsNodes().isNotEmpty() }
        if (compose.onAllNodesWithText("Começar", substring = true).fetchSemanticsNodes().isNotEmpty()) compose.onNodeWithText("Começar", substring = true).performClick()
    }
    @Test fun goalsEditSaveRestoreAndReturn() = runBlocking<Unit> {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as LottanApplication
        val repository = WorkspaceRepository(app)
        val before = repository.state.first()
        val showBefore = app.preferences.state.first().showGoals
        try {
            repository.goals(10, 70, 3); app.preferences.flag("showGoals", true)
            enter()
            compose.onNodeWithText("Editar metas").performScrollTo().performClick()
            compose.onNodeWithText("Metas de leitura", substring = false).assertExists()
            compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("25")
            compose.onNodeWithText("Salvar metas").performScrollTo().performClick()
            compose.waitUntil(5000) { compose.onAllNodesWithText("ODMANI", substring = false).fetchSemanticsNodes().isNotEmpty() }
            assertEquals(25, repository.state.first().daily)
            compose.onNodeWithText("Editar metas").performScrollTo().performClick()
            compose.onNodeWithText("Restaurar valores padrão").performScrollTo().performClick()
            compose.onNodeWithText("Restaurar metas?").assertExists()
            compose.onNodeWithText("Cancelar", substring = false).performClick()
            assertEquals(25, repository.state.first().daily)
            compose.onNodeWithText("Restaurar valores padrão").performClick()
            compose.onNodeWithText("Restaurar", substring = false).performClick()
            compose.onNodeWithText("Salvar metas").performScrollTo().performClick()
            compose.waitUntil(5000) { compose.onAllNodesWithText("ODMANI", substring = false).fetchSemanticsNodes().isNotEmpty() }
            val saved = repository.state.first()
            assertEquals(0, saved.daily); assertEquals(0, saved.weekly); assertEquals(0, saved.consistency)
            compose.onNodeWithText("Adicionar metas").assertExists()
        } finally { repository.goals(before.daily, before.weekly, before.consistency); app.preferences.flag("showGoals", showBefore) }
    }
    @Test fun searchChecklistPageAndReaderPanelRemainFunctional() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as LottanApplication
        val repository = WorkspaceRepository(app)
        app.preferences.hide(true); app.preferences.direction("single")
        val source = File(app.cacheDir, "correcoes-ui.pdf")
        PdfDocument().let { doc -> repeat(3) { i -> val p = doc.startPage(PdfDocument.PageInfo.Builder(200, 300, i + 1).create()); doc.finishPage(p) }; source.outputStream().use { doc.writeTo(it) }; doc.close() }
        val book = app.library.import(Uri.fromFile(source))
        app.library.dao.put(book.copy(name = "ODMANI-correcoes-teste.pdf"))
        repository.space(book.id, DocumentSpace("Nota preservada", "Checklist", listOf(TaskItem("check", "Revisar trecho", page = 0))))
        try {
            enter()
            compose.onNodeWithContentDescription("Biblioteca").performClick()
            compose.onNodeWithContentDescription("Pesquisar documentos").performClick()
            compose.onNode(hasSetTextAction()).performTextInput("nao-encontra-xyz")
            compose.onNodeWithText("ODMANI-correcoes-teste").assertDoesNotExist()
            compose.onNodeWithContentDescription("Limpar pesquisa").performClick()
            compose.onNodeWithTag("document-grid").performScrollToNode(hasText("ODMANI-correcoes-teste"))
            compose.onNodeWithText("ODMANI-correcoes-teste").performClick()
            compose.onAllNodesWithText("▶  Continuar leitura").assertCountEquals(0)
            compose.onNodeWithText("Ler", substring = false).assertExists()
            compose.onNodeWithText("Descrição").performScrollTo()
            compose.onNode(hasText("Revisar trecho") and hasSetTextAction()).performTextReplacement("Revisar trecho alterado")
            compose.onNode(hasText("Página opcional") and hasSetTextAction()).performScrollTo().performTextReplacement("999")
            compose.onNodeWithText("Use uma página de 1 a 3").assertExists()
            compose.onNode(hasText("Página opcional") and hasSetTextAction()).performTextReplacement("2")
            compose.onNodeWithText("Abrir página 2").performScrollTo().performClick()
            compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("Página 2").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription("Ajustes de leitura").performClick()
            compose.onNodeWithText("Ajustes de leitura", substring = false).assertExists()
            compose.onNodeWithText("Concluir").assertIsDisplayed()
            compose.onNodeWithText("Exibição das páginas").performScrollTo()
            compose.onNodeWithText("Concluir").assertIsDisplayed()
            compose.onNodeWithText("Página única", substring = false).performClick()
            compose.onNodeWithText("Concluir").performClick()
            compose.onNodeWithContentDescription("Página 2").performClick()
            compose.waitUntil(5000) { compose.onAllNodesWithContentDescription("Voltar").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithContentDescription("Página 2").assertExists().performClick()
            compose.waitUntil(5000) { compose.onAllNodesWithContentDescription("Voltar").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("‹", substring = false).performClick()
            compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("Página 1").fetchSemanticsNodes().isNotEmpty() }
            val saved = repository.state.first().spaces[book.id]!!
            assertEquals("Nota preservada", saved.note)
            assertEquals("Revisar trecho alterado", saved.tasks.single().text)
            assertEquals(1, saved.tasks.single().page)
        } finally { app.library.remove(book.id); source.delete() }
    }
}
