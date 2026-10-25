package br.com.lottan.nativeapp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
class MenuRegressionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun menuAndSettingsHaveBackWithoutRedundantHamburger() {
        compose.waitUntil(10_000) { compose.onAllNodesWithContentDescription("Biblioteca").fetchSemanticsNodes().isNotEmpty() || compose.onAllNodesWithText("Começar", substring = true).fetchSemanticsNodes().isNotEmpty() }
        if (compose.onAllNodesWithText("Começar", substring = true).fetchSemanticsNodes().isNotEmpty()) compose.onNodeWithText("Começar", substring = true).performClick()
        listOf("Explorar", "Atividade", "Biblioteca", "Início").forEach { label -> compose.onNodeWithContentDescription(label).performClick() }
        compose.onNodeWithContentDescription("Abrir menu").performClick()
        compose.onNodeWithText("Menu", substring = false).assertExists()
        compose.onAllNodesWithContentDescription("Abrir menu").assertCountEquals(0)
        compose.onNodeWithText("Páginas do PDF").performScrollTo().performClick()
        compose.onAllNodesWithContentDescription("Abrir menu").assertCountEquals(0)
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithText("Menu", substring = false).assertExists()
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("ODMANI", substring = false).assertExists()
    }
}
