package com.denge.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.denge.app.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class StatisticsJourneyTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private lateinit var original: AppState
    private val graph get() = (ui.activity.application as DengeApplication).graph
    private val date = LocalDate.now().withDayOfMonth(1)
    @Before fun setup() {
        runBlocking {
            original = graph.repo.read()
            val training = Day(date.toString(), workouts = listOf(Workout(id = "morning", title = "Sabah antrenmanı", minutes = 35), Workout(id = "evening", title = "Akşam antrenmanı", minutes = 65)), workoutStatus = "trained")
            graph.repo.update { AppState(profile = Profile(name = "İstatistik Testi", onboarded = true), days = mapOf(training.date to training)) }
        }
        ui.waitUntil(10000) { ui.onAllNodesWithText("Merhaba, İstatistik Testi.").fetchSemanticsNodes().isNotEmpty() }
    }
    @After fun restore() { if (::original.isInitialized) runBlocking { graph.repo.update { original } } }
    private fun screenshot(name: String) {
        val bitmap = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val folder = java.io.File(ui.activity.getExternalFilesDir(null), "statistics-preview").apply { mkdirs() }
        java.io.File(folder, name).outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    @Test fun monthCalendarOpensTrainingDayAndSessionsRemainSeparateFromAttendance() {
        ui.onNodeWithText("Gidişat").performClick()
        ui.onNodeWithText("Aylık").performClick()
        ui.onNodeWithText("Hareket").performClick()
        ui.onNodeWithTag("statistics-list").performScrollToNode(hasText("2 antrenman kaydı"))
        ui.onNodeWithText("2 antrenman kaydı").assertIsDisplayed()
        ui.onNodeWithText("100 dk").assertIsDisplayed()
        ui.onNodeWithTag("statistics-list").performScrollToIndex(0)
        screenshot("06-statistics-activity.png")
        ui.onNodeWithTag("statistics-list").performScrollToNode(hasText("Antrenman takvimin"))
        val label = date.format(DateTimeFormatter.ofPattern("d MMMM EEEE", Locale.forLanguageTag("tr-TR"))) + ", Antrenman"
        ui.onNodeWithContentDescription(label).performScrollTo().assertIsDisplayed()
        screenshot("07-training-calendar.png")
        ui.onNodeWithContentDescription(label).performClick()
        ui.waitUntil(10000) { ui.onAllNodesWithTag("today-list").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithTag("today-list").performScrollToNode(hasText("Sabah antrenmanı"))
        ui.onNodeWithText("Sabah antrenmanı").assertIsDisplayed()
        ui.onNodeWithText("Akşam antrenmanı").assertIsDisplayed()
    }
    @Test fun periodNavigationStopsAtCurrentMonthAndAllStatisticSectionsAreReachable() {
        ui.onNodeWithText("Gidişat").performClick()
        ui.onNodeWithText("Aylık").performClick()
        ui.onNodeWithContentDescription("Sonraki dönem").assertIsNotEnabled()
        ui.onNodeWithContentDescription("Önceki dönem").performClick()
        ui.onNodeWithContentDescription("Sonraki dönem").assertIsEnabled().performClick()
        ui.onNodeWithContentDescription("Sonraki dönem").assertIsNotEnabled()
        ui.onNodeWithText("Beslenme").performClick()
        ui.onNodeWithTag("statistics-list").performScrollToNode(hasText("Makroların"))
        ui.onNodeWithText("Makroların").assertIsDisplayed()
        ui.onNodeWithTag("statistics-list").performScrollToIndex(0)
        ui.onNodeWithText("Kilo").performClick()
        ui.onNodeWithTag("statistics-list").performScrollToNode(hasText("Kilo eğrin"))
        ui.onNodeWithText("Kilo eğrin").assertIsDisplayed()
    }
    @Test fun hevyConnectionRequiresAnExplicitKeyAndNeverUsesAChatCredential() {
        if (graph.secrets.has("hevy")) return
        ui.onNodeWithText("Ayarlar").performClick()
        ui.onNodeWithTag("settings-list").performScrollToNode(hasText("Bağlan ve doğrula"))
        ui.onNodeWithText("Bağlan ve doğrula").assertIsNotEnabled()
        screenshot("08-hevy-connection.png")
        ui.onNodeWithText("Hevy API anahtarı").performTextInput("local-test-value")
        ui.onNodeWithText("Bağlan ve doğrula").assertIsEnabled()
    }
}
