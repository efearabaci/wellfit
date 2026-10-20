package com.denge.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.denge.app.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import java.io.File
import java.time.LocalDate

/** Exercises the redesigned groups against real journal state; fixtures are always restored. */
class TodayDesignJourneyTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private lateinit var original: AppState
    private val graph get() = (ui.activity.application as DengeApplication).graph
    private val date get() = LocalDate.now()

    @Before fun saveOriginal() { original = runBlocking { graph.repo.read() } }
    @After fun restore() { if (::original.isInitialized) runBlocking { graph.repo.update { original } } }

    private fun seed(day: Day) {
        runBlocking { graph.repo.update { AppState(profile = Profile(name = "Efe", targetWeight = 76.0, onboarded = true), days = mapOf(day.date to day)) } }
        ui.waitUntil(10000) { ui.onAllNodesWithTag("today-summary").fetchSemanticsNodes().isNotEmpty() }
        ui.waitForIdle()
    }
    private fun screenshot(name: String) {
        ui.waitForIdle()
        // Semantics can settle a frame before the native surface is drawn.
        InstrumentationRegistry.getInstrumentation().uiAutomation.waitForIdle(350, 5000)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val folder = File(ui.activity.getExternalFilesDir(null), "today-preview").apply { mkdirs() }
        File(folder, name).outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    private fun populated() = Day(date.toString(), steps = 6420, weight = 78.4,
        meals = listOf(
            Meal(name = "Yulaf, yoğurt ve yaban mersini", category = "Kahvaltı", time = "08:30", calories = 425, protein = 26.0, carbs = 52.0, fat = 12.0),
            Meal(name = "Izgara tavuk ve bulgur", category = "Öğle", time = "13:15", calories = 680, protein = 48.0, carbs = 72.0, fat = 20.0),
            Meal(name = "Badem ve kefir", category = "Ara öğün", time = "16:00", calories = 240, protein = 12.0, carbs = 18.0, fat = 14.0, aiSourceText = "Badem ve kefir")
        ), workouts = listOf(Workout(title = "Üst vücut", minutes = 55, source = "Hevy", notes = "Bench press · 3 × 10")), workoutStatus = "trained", note = "Antrenman 18:30'da. Su tüketimi: 2 litre.")

    @Test fun populatedSectionsRemainReachableAndClosedDayDisablesEditing() {
        seed(populated())
        ui.onNodeWithText("1.345").assertIsDisplayed()
        screenshot("01-today-summary.png")
        ui.onNodeWithTag("today-list").performScrollToNode(hasText("Izgara tavuk ve bulgur"))
        ui.onNodeWithText("Izgara tavuk ve bulgur").assertIsDisplayed()
        ui.onNodeWithTag("today-list").performScrollToIndex(1)
        screenshot("02-today-meals.png")
        ui.onNodeWithTag("today-list").performScrollToNode(hasText("Üst vücut"))
        ui.onNodeWithText("Üst vücut").assertIsDisplayed()
        ui.onNodeWithTag("today-list").performScrollToIndex(2)
        screenshot("03-today-training.png")
        ui.onNodeWithTag("today-list").performScrollToIndex(0)
        seed(populated().copy(closed = true))
        ui.onNodeWithTag("meal-fab").assertDoesNotExist()
        ui.onNodeWithText("Gün kapalı").assertIsDisplayed()
        screenshot("04-today-closed.png")
        ui.onNodeWithTag("today-list").performScrollToNode(hasText("Günü yeniden aç"))
        ui.onNodeWithText("Günü yeniden aç").assertIsDisplayed()
        ui.onAllNodes(hasText("Hevy'den al") and hasClickAction()).assertCountEquals(0)
    }

    @Test fun emptyAndZeroMeasurementsAndRestDayRemainDistinct() {
        seed(Day(date.toString()))
        ui.onAllNodesWithText("—").assertCountEquals(2)
        screenshot("05-today-empty.png")
        ui.onNodeWithTag("today-list").performScrollToNode(hasText("Antrenman durumu belirtilmedi."))
        ui.onNodeWithText("Antrenman durumu belirtilmedi.").assertIsDisplayed()
        ui.onNodeWithText("Bugün dinleniyorum").performScrollTo().performClick()
        ui.waitUntil(10000) { runBlocking { graph.repo.read().day(date).workoutStatus == "rest" } }
        ui.onNodeWithText("Dinlenme günü").assertIsDisplayed()
        ui.waitUntil(10000) { ui.onAllNodesWithText("Dinlenme günü kaydedildi").fetchSemanticsNodes().isEmpty() }
        ui.onNodeWithTag("today-list").performScrollToIndex(2)
        screenshot("06-today-rest.png")
        runBlocking { graph.repo.update { it.copy(days = mapOf(date.toString() to Day(date.toString(), steps = 0))) } }
        ui.onNodeWithTag("today-list").performScrollToIndex(0)
        // A recorded zero is visible as zero, while the missing weight remains unknown.
        ui.onAllNodesWithText("—").assertCountEquals(1)
    }
}
