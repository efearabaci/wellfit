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
        ui.waitUntil(10000) { ui.onAllNodesWithTag("today-summary").fetchSemanticsNodes().isNotEmpty() }
    }
    @After fun restore() { if (::original.isInitialized) runBlocking { graph.repo.update { original } } }
    private fun month() {
        ui.onNodeWithTag("period-picker").performClick()
        ui.onNodeWithText("Ay görünümü").performClick()
    }
    @Test fun monthCalendarOpensTrainingDayAndSessionsRemainSeparateFromAttendance() {
        ui.onNodeWithText("Gidişat").performClick()
        month()
        ui.onNodeWithTag("statistics-list").performScrollToNode(hasTestTag("progress-training"))
        ui.onNodeWithText("Seans").assertIsDisplayed()
        ui.onNodeWithText("100").assertIsDisplayed()
        val label = date.format(DateTimeFormatter.ofPattern("d MMMM EEEE", Locale.forLanguageTag("tr-TR"))) + ", Antrenman"
        ui.onNodeWithContentDescription(label).performScrollTo().assertIsDisplayed().performClick()
        ui.waitUntil(10000) { ui.onAllNodesWithTag("today-list").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithTag("today-list").performScrollToNode(hasText("Sabah antrenmanı"))
        ui.onNodeWithText("Sabah antrenmanı").assertIsDisplayed()
        ui.onNodeWithText("Akşam antrenmanı").assertIsDisplayed()
    }
    @Test fun periodNavigationStopsAtCurrentMonthAndEmptyMetricsAreOmitted() {
        ui.onNodeWithText("Gidişat").performClick()
        month()
        ui.onNodeWithContentDescription("Sonraki dönem").assertIsNotEnabled()
        ui.onNodeWithContentDescription("Önceki dönem").performClick()
        ui.onNodeWithTag("progress-empty").assertIsDisplayed()
        ui.onNodeWithTag("progress-comparison").assertDoesNotExist()
        ui.onNodeWithContentDescription("Sonraki dönem").assertIsEnabled().performClick()
        ui.onNodeWithContentDescription("Sonraki dönem").assertIsNotEnabled()
        ui.onNodeWithTag("progress-training").assertExists()
        ui.onNodeWithTag("progress-nutrition").assertDoesNotExist()
        ui.onNodeWithTag("progress-steps").assertDoesNotExist()
        ui.onNodeWithTag("progress-weight").assertDoesNotExist()
        ui.onNodeWithText("Kayıt devamlılığın").assertDoesNotExist()
        ui.onNodeWithText("Genel").assertDoesNotExist()
    }
    @Test fun hevyConnectionRequiresAnExplicitKeyAndNeverUsesAChatCredential() {
        if (graph.secrets.has("hevy")) return
        ui.onNodeWithContentDescription("Ayarlar").performClick()
        ui.onNodeWithTag("settings-list").performScrollToNode(hasText("Bağlan ve doğrula"))
        ui.onNodeWithText("Bağlan ve doğrula").assertIsNotEnabled()
        ui.onNodeWithText("Hevy API anahtarı").performTextInput("local-test-value")
        ui.onNodeWithText("Bağlan ve doğrula").assertIsEnabled()
    }
}
