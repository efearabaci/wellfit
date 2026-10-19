package com.denge.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import com.denge.app.domain.*
import com.denge.app.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import java.time.LocalDate

class AiMealJourneyTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private lateinit var original: AppState
    private val graph get() = (ui.activity.application as DengeApplication).graph
    private val vm get() = ViewModelProvider(ui.activity)[DengeViewModel::class.java]
    @Before fun setup() {
        runBlocking { original = graph.repo.read(); graph.repo.update { AppState(profile = Profile(name = "Test", onboarded = true)) } }
        ui.waitUntil(10000) { ui.onAllNodesWithText("Merhaba, Test.").fetchSemanticsNodes().isNotEmpty() }
    }
    @After fun restore() { if (::original.isInitialized) runBlocking { graph.repo.update { original } } }
    private fun openAi() {
        ui.onNodeWithTag("today-list").performScrollToNode(hasText("Öğün ekle"))
        ui.onNodeWithText("Öğün ekle").performClick()
        ui.onNodeWithText("AI ile anlat").performClick()
    }
    private fun screenshot(name: String) {
        val bitmap = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val folder = java.io.File(ui.activity.getExternalFilesDir(null), "meal-preview").apply { mkdirs() }
        java.io.File(folder, name).outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    @Test fun freeTextDraftSurvivesActivityRecreationWithoutSendingAnApiRequest() {
        openAi()
        ui.onNodeWithText("Ne yedin, ne kadar?").performTextInput("150 g tavuk ve bir bardak ayran")
        ui.activityRule.scenario.recreate()
        ui.onNodeWithText("Ne yedin, ne kadar?").assertTextContains("150 g tavuk ve bir bardak ayran")
        assertTrue(runBlocking { graph.repo.read().day(LocalDate.now()).meals.isEmpty() })
    }
    @Test fun preparedMealsCanBeEditedAndOnlyCommitAfterExplicitSave() {
        openAi()
        // A deterministic service result exercises the real review, editor and repository path without a paid API key.
        val meals = UnderstoodMeals(listOf(
            UnderstoodFood("Tavuk ve pilav", "Öğle", "13:00", 650, 45.0, null, 18.0, "150 g tavuk ve bir porsiyon pilav varsayıldı."),
            UnderstoodFood("Ayran", "Öğle", "13:00", 70, 4.0, 5.0, 3.0, "200 ml ayran varsayıldı.")
        ), "Porsiyonları kontrol et.").toMeals("150 g tavuk, bir tabak pilav ve ayran yedim")
        ui.runOnIdle { vm.aiMealDraft.value = AiMealDraft("150 g tavuk, bir tabak pilav ve ayran yedim", meals, "Porsiyonları kontrol et.") }
        ui.onNodeWithText("Kontrol et ve ekle").performScrollTo().assertIsDisplayed()
        screenshot("09-ai-meal-review.png")
        assertTrue(runBlocking { graph.repo.read().day(LocalDate.now()).meals.isEmpty() })
        ui.onAllNodesWithText("Düzenle").onFirst().performScrollTo().performClick()
        ui.onNodeWithText("Kalori · kcal").performTextReplacement("600")
        ui.onNodeWithText("Öğünü kaydet").performScrollTo().performClick()
        assertTrue(runBlocking { graph.repo.read().day(LocalDate.now()).meals.isEmpty() })
        ui.onNodeWithText("2 öğünü günlüğe ekle").performScrollTo().performClick()
        ui.waitUntil(10000) { runBlocking { graph.repo.read().day(LocalDate.now()).meals.size == 2 } }
        val day = runBlocking { graph.repo.read().day(LocalDate.now()) }
        assertEquals(670, day.calories); assertNull(day.carbs)
        assertEquals("150 g tavuk, bir tabak pilav ve ayran yedim", day.meals.first().aiSourceText)
        ui.waitUntil(10000) { vm.aiMealDraft.value.meals.isEmpty() }
    }
}
