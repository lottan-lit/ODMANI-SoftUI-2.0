package br.com.lottan.nativeapp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class NavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun libraryAndOrganizationAreReachable() {
        compose.waitUntil(5_000) {
            compose
                .onAllNodesWithText("Começar", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty() ||
                compose
                    .onAllNodesWithContentDescription("Biblioteca")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
        }
        if (
            compose
                .onAllNodesWithText("Começar", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        )
            compose.onNodeWithText("Começar", substring = true).performClick()
        compose.waitUntil(5_000) {
            compose
                .onAllNodesWithContentDescription("Biblioteca")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNodeWithContentDescription("Biblioteca").performClick()
        compose.onNodeWithContentDescription("Ver todas as pastas").performClick()
        compose.onNodeWithText("Organizar PDFs").assertIsDisplayed()
    }
}
