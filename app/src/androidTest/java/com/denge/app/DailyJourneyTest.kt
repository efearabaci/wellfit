package com.denge.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.denge.app.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.time.LocalDate

class DailyJourneyTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private lateinit var original: AppState
    private val graph get() = (ui.activity.application as DengeApplication).graph
    @Before fun setup() {
        runBlocking { original = graph.repo.read(); graph.repo.update { AppState(profile = Profile(name = "Test", onboarded = true)) } }
        ui.waitUntil(10000) { ui.onAllNodesWithTag("today-summary").fetchSemanticsNodes().isNotEmpty() }
    }
    @After fun restore() { runBlocking { graph.repo.update { original } } }
    @Test fun recordMealCloseDayAndReviewExactMemory() {
        ui.onNodeWithTag("meal-fab").performClick()
        ui.onNodeWithText("Ne yedin?").performTextInput("Tavuk ve pilav")
        ui.onNodeWithText("Kalori · kcal").performTextInput("650")
        ui.onNodeWithContentDescription("Protein · g").performTextInput("45")
        ui.onNodeWithText("Öğünü kaydet").performScrollTo().performClick()
        ui.waitUntil(10000) { runBlocking { graph.repo.read().day(LocalDate.now()).meals.size == 1 } }
        ui.onNodeWithTag("today-list").performScrollToNode(hasText("Tavuk ve pilav"))
        ui.onNodeWithText("Tavuk ve pilav").assertIsDisplayed()
        ui.onNodeWithTag("today-list").performScrollToNode(hasText("Günü kapat"))
        ui.waitUntil(10000) { ui.onAllNodes(hasText("Günü kapat") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        ui.waitUntil(10000) { ui.onAllNodesWithText("Öğün kaydedildi").fetchSemanticsNodes().isEmpty() }
        ui.onNodeWithText("Günü kapat").performClick()
        ui.waitUntil(10000) { ui.onAllNodesWithText("Daha sonra yeniden açıp düzenleyebilirsin.").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithText("Daha sonra yeniden açıp düzenleyebilirsin.").assertIsDisplayed()
        ui.onAllNodesWithText("Günü kapat").onLast().performClick()
        ui.waitUntil(10000) { runBlocking { graph.repo.read().day(LocalDate.now()).closed } }
        val day = runBlocking { graph.repo.read().day(LocalDate.now()) }
        assertEquals(650, day.calories); assertTrue(day.recap!!.prompt.contains("Tavuk ve pilav"))
        assertTrue(day.recap.prompt.contains("completeHistory"))
        assertNull(day.meals.single().carbs); assertNull(day.meals.single().fat)
        assertTrue(day.recap.prompt.contains("\"carbs\":null"))
    }
}
