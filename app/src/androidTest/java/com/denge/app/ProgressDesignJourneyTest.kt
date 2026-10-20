package com.denge.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.platform.app.InstrumentationRegistry
import com.denge.app.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import java.io.File
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek

class ProgressDesignJourneyTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private lateinit var original: AppState
    private val graph get() = (ui.activity.application as DengeApplication).graph
    private val today = LocalDate.now()
    private val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    @Before fun saveOriginal() { original = runBlocking { graph.repo.read() } }
    @After fun restore() { if (::original.isInitialized) runBlocking { graph.repo.update { original } } }
    private fun seed(days: List<Day>) {
        runBlocking { graph.repo.update { AppState(profile = Profile(name = "Efe", onboarded = true, targetWeight = 76.0), days = days.associateBy { it.date }) } }
        ui.waitUntil(10000) { ui.onAllNodesWithTag("today-summary").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithText("Gidişat").performClick()
    }
    private fun screenshot(name: String) {
        ui.waitForIdle()
        InstrumentationRegistry.getInstrumentation().uiAutomation.waitForIdle(400, 5000)
        val bitmap = if (name == "06-period-picker.png") {
            ui.onNodeWithText("Ay görünümü").assertIsDisplayed()
            InstrumentationRegistry.getInstrumentation().uiAutomation.waitForIdle(600, 5000)
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        } else ui.onAllNodes(isRoot()).onLast().captureToImage().asAndroidBitmap()
        val folder = File(ui.activity.getExternalFilesDir(null), "progress-redesign").apply { mkdirs() }
        File(folder, name).outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    private fun scrollTo(tag: String) { ui.onNodeWithTag("statistics-list").performScrollToNode(hasTestTag(tag)) }
    private fun meal(calories: Int = 2050, carbs: Double? = 210.0, fat: Double? = 65.0) = Meal(name = "Günlük öğünler", category = "Öğle", time = "13:00", calories = calories, protein = 148.0, carbs = carbs, fat = fat)
    private fun fixtures(): List<Day> {
        val first = Day(monday.toString(), meals = listOf(meal()), closed = true, steps = 9200, weight = 78.4, workouts = listOf(Workout(title = "Üst vücut", minutes = 55), Workout(title = "Akşam yürüyüşü", kind = "Yürüyüş", minutes = 30)), workoutStatus = "trained")
        val current = if (today == monday) emptyList() else listOf(Day(today.toString(), meals = listOf(meal(1345)), steps = 6420, weight = 78.1, workoutStatus = "rest"))
        val previous = (1L..3L).map { offset -> Day(monday.minusDays(offset).toString(), meals = listOf(meal(2200)), closed = true, steps = 7000, weight = 78.8, workoutStatus = "rest") }
        return listOf(first) + current + previous
    }
    @Test fun unifiedSectionsAndOnlyValidComparisonsRemainReachable() {
        seed(fixtures())
        screenshot("01-progress-overview.png")
        ui.onNodeWithText("Genel").assertDoesNotExist()
        ui.onNodeWithText("Haftalık").assertDoesNotExist()
        ui.onNodeWithText("Kapalı gün").assertDoesNotExist()
        scrollTo("progress-training")
        ui.onNodeWithText("Seans").assertExists()
        ui.onNodeWithTag("statistics-list").performScrollToIndex(2)
        ui.onNodeWithTag("progress-steps").assertIsDisplayed()
        if (today > monday) {
            val layouts = mutableListOf<TextLayoutResult>()
            ui.onNodeWithText("10.000").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            org.junit.Assert.assertTrue("Chart tick labels must fit on one line", layouts.isNotEmpty() && layouts.all { it.lineCount == 1 })
        }
        screenshot("02-progress-steps.png")
        scrollTo("progress-nutrition")
        ui.onNodeWithTag("statistics-list").performScrollToIndex(3)
        screenshot("03-progress-nutrition.png")
        scrollTo("progress-weight")
        ui.onNodeWithTag("statistics-list").performScrollToIndex(4)
        screenshot("04-progress-weight.png")
        scrollTo("progress-comparison")
        ui.onNodeWithTag("statistics-list").performScrollToIndex(5)
        ui.onNodeWithText("Karşılaştırma").assertIsDisplayed()
        screenshot("05-progress-comparison.png")
        ui.onNodeWithTag("statistics-list").performScrollToIndex(0)
        ui.onNodeWithTag("period-picker").performClick()
        screenshot("06-period-picker.png")
        ui.onNodeWithText("Ay görünümü").performClick()
        ui.onNodeWithContentDescription("Sonraki dönem").assertIsNotEnabled()
        screenshot("07-progress-month.png")
    }
    @Test fun emptyPeriodShowsOneActionAndNoPlaceholderStatistics() {
        seed(emptyList())
        ui.onNodeWithTag("progress-empty").assertIsDisplayed()
        listOf("progress-training", "progress-nutrition", "progress-steps", "progress-weight", "progress-comparison").forEach { ui.onNodeWithTag(it).assertDoesNotExist() }
        ui.onNodeWithText("Kayıt devamlılığın").assertDoesNotExist()
        screenshot("08-progress-empty.png")
        ui.onNodeWithText("Bugünün kaydını aç").performClick()
        ui.onNodeWithTag("today-summary").assertIsDisplayed()
    }
    @Test fun missingMacrosAndSingleWeightDoNotProduceInventedValuesOrComparison() {
        seed(listOf(Day(today.toString(), closed = true, meals = listOf(meal(carbs = null, fat = null)), weight = 78.4)))
        ui.onNodeWithTag("statistics-list").performScrollToIndex(1)
        ui.onNodeWithText("Protein").assertExists()
        ui.onNodeWithText("Karb.").assertDoesNotExist()
        ui.onNodeWithText("Yağ").assertDoesNotExist()
        ui.onNodeWithTag("progress-training").assertDoesNotExist()
        ui.onNodeWithTag("progress-steps").assertDoesNotExist()
        scrollTo("progress-weight")
        ui.onNodeWithText("1 ölçüm").assertExists()
        ui.onNodeWithTag("progress-comparison").assertDoesNotExist()
        screenshot("09-progress-single-weight.png")
    }
    @Test fun recordedZeroStepsRemainARealMeasurementAndChartOpensTheDay() {
        seed(listOf(Day(today.toString(), steps = 0)))
        ui.onNodeWithTag("progress-steps").assertIsDisplayed()
        ui.onNode(hasText("0 adım") and hasClickAction()).performClick()
        ui.waitUntil(10000) { ui.onAllNodesWithTag("today-summary").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithTag("today-summary").assertIsDisplayed()
    }
}
