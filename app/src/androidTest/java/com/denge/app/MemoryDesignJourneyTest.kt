package com.denge.app

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.denge.app.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MemoryDesignJourneyTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private lateinit var original: AppState
    private val graph get() = (ui.activity.application as DengeApplication).graph
    private val today = LocalDate.now()
    private val created = Instant.now().minusSeconds(600).toString()
    private val oldCreated = Instant.now().minusSeconds(3600).toString()
    private val report = """
        # Bugünün özeti
        **1.980 kcal** ve **142 g protein** kaydettin. Adım sayın **8.240**.

        Üst vücut antrenmanın 55 dakika sürdü.
        ## Bu hafta
        İki antrenman kaydı var. Adım ölçümü iki günde bulunuyor.
        ## Bu ay
        İki kilo ölçümün var: **78,6 kg** ve **78,4 kg**.
        ## Sonraki gün için öneriler
        - Öğün kaydında porsiyon miktarını gram olarak belirt.
        - Adım sayını gün sonunda güncelle.
    """.trimIndent()
    private fun recap(text: String = report, at: String = created) = Recap(createdAt = at, model = "deepseek-flash", status = "ready", text = text, snapshotGzip = SnapshotCodec.pack("{\"fixture\":\"local-memory-fixture\",\"steps\":8240}"))
    private fun readyDay() = Day(today.toString(), zone = "Europe/Istanbul", closed = true, steps = 8240, weight = 78.4,
        meals = listOf(Meal(name = "Günlük öğünler", category = "Öğle", time = "13:00", calories = 1980, protein = 142.0, carbs = 210.0, fat = 62.0)),
        workouts = listOf(Workout(title = "Üst vücut", minutes = 55)), workoutStatus = "trained", note = "Akşam yürüyüşü: 30 dakika.", recap = recap(),
        archivedRecaps = listOf(recap("# Bugünün özeti\nÖnceki sürümde 1.800 kcal kaydı vardı.\n## Bu hafta\nÖnceki haftalık değerlendirme.", oldCreated)))
    @Before fun saveOriginal() { original = runBlocking { graph.repo.read() } }
    @After fun restore() { if (::original.isInitialized) runBlocking { graph.repo.update { original } } }
    private fun seed(days: List<Day>, notes: String = "Antrenman günlerim pazartesi, çarşamba ve cuma. Laktozsuz ürünleri tercih ediyorum.") {
        runBlocking { graph.repo.update { AppState(profile = Profile(name = "Efe", onboarded = true, memory = notes), days = days.associateBy { it.date }) } }
        ui.waitUntil(10000) { ui.onAllNodesWithTag("today-summary").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithText("Hafıza").performClick()
        ui.waitUntil(10000) { ui.onAllNodesWithTag("memory-list").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun screenshot(name: String, dialog: Boolean = false) {
        ui.waitForIdle()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.waitForIdle(400, 5000)
        val bitmap = if (dialog) automation.takeScreenshot() else ui.onAllNodes(isRoot()).onLast().captureToImage().asAndroidBitmap()
        val folder = File(ui.activity.getExternalFilesDir(null), "memory-redesign").apply { mkdirs() }
        File(folder, name).outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
    private fun scrollTo(tag: String) = ui.onNodeWithTag("memory-list").performScrollToNode(hasTestTag(tag))
    private fun archiveLabel() = Instant.parse(oldCreated).atZone(ZoneId.of("Europe/Istanbul")).format(DateTimeFormatter.ofPattern("d MMM · HH:mm", Locale.forLanguageTag("tr-TR")))

    @Test fun readyReviewHistoryAndArchivedVersionsRemainReadable() {
        val yesterday = readyDay().copy(date = today.minusDays(1).toString(), weight = 78.6, steps = 7600, recap = recap(report.replace("Üst vücut antrenmanın 55 dakika sürdü.", "Dünkü değerlendirme.")), archivedRecaps = emptyList())
        seed(listOf(readyDay(), yesterday))
        ui.onNodeWithTag("memory-article-day").assertIsDisplayed()
        ui.onNodeWithText("AI değerlendirmesi").assertIsDisplayed()
        ui.onNodeWithContentDescription("Hafızayı dışa aktar").assertIsEnabled()
        screenshot("01-memory-overview.png")
        ui.onNodeWithTag("memory-list").performScrollToIndex(3)
        screenshot("02-memory-periods.png")
        ui.onNodeWithTag("memory-list").performScrollToIndex(5)
        ui.onNodeWithTag("memory-article-next").assertIsDisplayed()
        screenshot("03-memory-suggestions.png")
        scrollTo("memory-records")
        ui.onNodeWithTag("memory-list").performScrollToIndex(6)
        ui.onNodeWithText("Gün notu").performClick()
        ui.onNodeWithText("Akşam yürüyüşü: 30 dakika.").assertExists()
        screenshot("04-memory-records.png")
        scrollTo("memory-archive")
        ui.onNodeWithText(archiveLabel()).performClick()
        ui.onNodeWithText("Önceki sürümde 1.800 kcal kaydı vardı.").assertExists()
        ui.onNodeWithTag("memory-list").performScrollToIndex(7)
        screenshot("05-memory-archive.png")
        ui.onNodeWithTag("memory-list").performScrollToIndex(0)
        ui.onNodeWithTag("memory-history-picker").performClick()
        ui.onNodeWithText("Değerlendirmeler").assertIsDisplayed()
        screenshot("06-memory-history.png", dialog = true)
        ui.onNodeWithText(today.minusDays(1).format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("tr-TR")))).performClick()
        ui.onNodeWithText("Dünkü değerlendirme.", substring = true).assertIsDisplayed()
        screenshot("07-memory-previous-day.png")
        ui.activityRule.scenario.recreate()
        ui.onNodeWithText("Dünkü değerlendirme.", substring = true).assertIsDisplayed()
    }
    @Test fun emptyMemoryHasOneClearActionWithoutSyntheticMetrics() {
        seed(emptyList(), notes = "")
        ui.onNodeWithText("Henüz değerlendirme yok").assertIsDisplayed()
        listOf("memory-records", "memory-archive", "memory-profile-note", "memory-article-day").forEach { ui.onNodeWithTag(it).assertDoesNotExist() }
        ui.onNodeWithText("Wellfit hafızası").assertDoesNotExist()
        screenshot("08-memory-empty.png")
        ui.onNodeWithText("Günün kaydını aç").performClick()
        ui.onNodeWithTag("today-summary").assertIsDisplayed()
    }
    @Test fun reopenedDayKeepsArchivedReviewAndReturnsToTheSelectedJournal() {
        seed(listOf(readyDay().copy(closed = false, recap = null)))
        ui.onNodeWithTag("memory-status-open").assertIsDisplayed()
        ui.onNodeWithTag("memory-article-day").assertDoesNotExist()
        scrollTo("memory-archive")
        ui.onNodeWithText(archiveLabel()).performClick()
        ui.onNodeWithText("Önceki sürümde 1.800 kcal kaydı vardı.").assertExists()
        ui.onNodeWithTag("memory-list").performScrollToIndex(0)
        ui.onNodeWithText("Günün kaydını aç").performClick()
        ui.onNodeWithTag("today-summary").assertIsDisplayed()
        ui.onNodeWithText("Gün açık").assertIsDisplayed()
    }
    @Test fun auditShowsTheExactSnapshotAndNotesCanOpenSettings() {
        seed(listOf(readyDay()))
        ui.onNodeWithTag("memory-list").performScrollToNode(hasText("AI'ya gönderilen veri"))
        ui.onNodeWithText("AI'ya gönderilen veri").performClick()
        ui.onNodeWithText("Gönderim kaydı").assertIsDisplayed()
        ui.onAllNodes(hasScrollAction()).onLast().performScrollToNode(hasText("local-memory-fixture", substring = true))
        ui.onNodeWithText("local-memory-fixture", substring = true).assertExists()
        screenshot("09-memory-audit.png", dialog = true)
        ui.onNodeWithContentDescription("Kapat").performClick()
        scrollTo("memory-profile-note")
        ui.onNodeWithContentDescription("AI notlarını düzenle").performClick()
        ui.onNodeWithTag("settings-list").assertExists()
        ui.onNodeWithContentDescription("Geri").performClick()
        ui.onNodeWithTag("memory-list").assertExists()
    }
}
