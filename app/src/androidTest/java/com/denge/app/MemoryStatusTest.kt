package com.denge.app

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.denge.app.domain.*
import com.denge.app.ui.*
import org.junit.*
import org.junit.Assert.assertEquals
import java.time.LocalDate

/** Status actions are verified without keys, network requests, or writes to the journal. */
class MemoryStatusTest {
    @get:Rule val ui = createComposeRule()
    private val date = LocalDate.now()
    private fun recap(status: String, text: String = "", error: String = "") = Recap(createdAt = "2026-10-20T18:00:00Z", snapshotGzip = "", model = "test-model", status = status, text = text, error = error)
    @Test fun connectionActionsOpenSettingsAndSavedReviewRemainsReadableWithoutConnection() {
        var profile by mutableStateOf(Profile(onboarded = true))
        var day by mutableStateOf(Day(date.toString(), closed = true, recap = recap("pending")))
        var settingsCount = 0
        ui.setContent { DengeTheme { MemoryScreen(AppState(profile = profile, days = mapOf(day.date to day)), date, false, {}, {}, {}, {}, hasAiKey = false, openDay = {}, settings = { settingsCount++ }) } }
        ui.onNodeWithTag("memory-status-off").assertIsDisplayed()
        ui.onNodeWithText("AI ayarlarını aç").performClick()
        assertEquals(1, settingsCount)
        ui.runOnIdle { profile = profile.copy(aiConsent = true) }
        ui.onNodeWithTag("memory-status-key_required").assertIsDisplayed()
        ui.onNodeWithText("Yeniden dene").assertDoesNotExist()
        ui.runOnIdle { profile = profile.copy(aiConsent = false); day = day.copy(recap = recap("ready", "# Bugünün özeti\nKaydedilmiş değerlendirme.")) }
        ui.onNodeWithTag("memory-article-day").assertIsDisplayed()
        ui.onNodeWithText("Kaydedilmiş değerlendirme.").assertIsDisplayed()
        ui.onNodeWithText("AI ayarlarını aç").assertDoesNotExist()
    }
    @Test fun runningAndWaitingCannotDuplicateRequestsWhileFailureOffersRetry() {
        var day by mutableStateOf(Day(date.toString(), closed = true, recap = recap("running")))
        var busy by mutableStateOf(false)
        var retries = 0
        ui.setContent { DengeTheme { MemoryScreen(AppState(profile = Profile(onboarded = true, aiConsent = true), days = mapOf(day.date to day)), date, busy, { retries++ }, {}, {}, {}, hasAiKey = true, openDay = {}, settings = {}) } }
        ui.onNodeWithTag("memory-status-running").assertIsDisplayed()
        ui.onNodeWithText("Yeniden dene").assertDoesNotExist()
        ui.runOnIdle { day = day.copy(recap = recap("pending")) }
        ui.onNodeWithTag("memory-status-waiting").assertIsDisplayed()
        ui.onNodeWithText("Yeniden dene").assertDoesNotExist()
        ui.runOnIdle { day = day.copy(recap = recap("failed", error = "Servis yanıt vermedi.")) }
        ui.onNodeWithTag("memory-status-failed").assertIsDisplayed()
        ui.onNodeWithText("Servis yanıt vermedi.").assertIsDisplayed()
        ui.onNodeWithText("Yeniden dene").performClick()
        assertEquals(1, retries)
        ui.runOnIdle { busy = true }
        ui.onNodeWithText("Yeniden dene").assertIsNotEnabled()
        assertEquals(1, retries)
    }
}
