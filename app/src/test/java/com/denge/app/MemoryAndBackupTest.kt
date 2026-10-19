package com.denge.app

import com.denge.app.data.BackupCodec
import com.denge.app.domain.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class MemoryAndBackupTest {
    private val date = LocalDate.of(2026, 10, 19)
    private fun meal(name: String = "Mercimek çorbası") = Meal(name = name, category = "Öğle", time = "12:30", calories = 300, protein = 15.0, carbs = 40.0, fat = 9.0)
    @Test fun `all historic facts and raw hevy sets are retained in prompt`() {
        val historical = Day("2024-01-01", meals = listOf(meal("Eski kayıt")), workouts = listOf(Workout(title = "Kuvvet", minutes = 45, rawJson = "{\"reps\":12,\"weight_kg\":40}")), closed = true)
        val today = Day(date.toString(), meals = listOf(meal()), closed = true)
        val future = Day("2026-10-20", note = "Gelecek veri")
        val state = AppState(profile = Profile(memory = "Haftada 3 gün"), days = listOf(historical, today, future).associateBy { it.date })
        val prompt = MemoryBuilder.payload(state, date)
        assertTrue(prompt.contains("Eski kayıt")); assertTrue(prompt.contains("weight_kg")); assertTrue(prompt.contains("Haftada 3 gün")); assertFalse(prompt.contains("Gelecek veri"))
        assertEquals(2, JsonCodec.parseToJsonElement(prompt).jsonObject.getValue("completeHistory").jsonArray.size)
    }
    @Test fun `missing days and open days do not become zero calorie closed days`() {
        val state = AppState(days = mapOf("2026-10-19" to Day("2026-10-19", meals = listOf(meal()), closed = true), "2026-10-18" to Day("2026-10-18", meals = listOf(meal()))))
        val root = JsonCodec.parseToJsonElement(MemoryBuilder.payload(state, date)).jsonObject
        val month = root.getValue("month").jsonObject
        assertEquals(1, month.getValue("closedDays").jsonPrimitive.int)
        assertEquals(300.0, month.getValue("closedDayCaloriesAverage").jsonPrimitive.double, .01)
        assertEquals(0, month.getValue("knownStepDays").jsonPrimitive.int)
        assertEquals("2026-10-19", root.getValue("week").jsonObject.getValue("start").jsonPrimitive.content)
    }
    @Test fun `prior prompt snapshots are excluded while previous results remain`() {
        val recap = Recap(createdAt = "2026-10-19T20:00:00Z", snapshotGzip = SnapshotCodec.pack("recursive-secret-marker"), model = "test", status = "ready", text = "Önceki özet")
        val state = AppState(days = mapOf(date.toString() to Day(date.toString(), archivedRecaps = listOf(recap))))
        val payload = MemoryBuilder.payload(state, date)
        assertTrue(payload.contains("Önceki özet")); assertFalse(payload.contains("recursive-secret-marker"))
    }
    @Test fun `oversized context fails rather than silently dropping facts`() {
        val state = AppState(profile = Profile(memory = "a".repeat(MemoryBuilder.MAX_PROMPT_CHARS)))
        assertThrows(IllegalArgumentException::class.java) { MemoryBuilder.payload(state, date) }
    }
    @Test fun `backup round trips Turkish data notes prompts and workout sets`() {
        val state = AppState(profile = Profile(name = "Çağrı", memory = "İş çıkışı antrenman"), days = mapOf(date.toString() to Day(date.toString(), meals = listOf(meal()), steps = 8021, weight = 82.4)))
        assertEquals(state, BackupCodec.decode(BackupCodec.encode(state)))
    }
    @Test fun `damaged backups are rejected`() {
        val valid = BackupCodec.encode(AppState())
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(valid.replace("denge-backup-v1", "broken")) }
        val corrupt = JsonCodec.parseToJsonElement(valid).jsonObject.toMutableMap().apply { put("sha256", JsonPrimitive("invalid")) }
        assertThrows(IllegalArgumentException::class.java) { BackupCodec.decode(JsonObject(corrupt).toString()) }
    }
    @Test fun `closed day cannot be edited`() {
        val state = AppState(days = mapOf(date.toString() to Day(date.toString(), closed = true)))
        assertThrows(IllegalArgumentException::class.java) { state.changeDay(date.toString()) { it.copy(steps = 0) } }
    }
    @Test fun `compressed snapshots preserve exact Unicode without data loss`() {
        val prompt = "İstanbul · öğünler 🥗 ıİşŞ".repeat(1000)
        val packed = SnapshotCodec.pack(prompt)
        assertEquals(prompt, SnapshotCodec.unpack(packed)); assertTrue(packed.length < prompt.length / 5)
    }
    @Test fun `negative and nonfinite macros are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { meal().copy(protein = Double.NaN).validate() }
        assertThrows(IllegalArgumentException::class.java) { meal().copy(calories = -1).validate() }
        assertThrows(java.time.format.DateTimeParseException::class.java) { meal().copy(time = "25:90").validate() }
    }
}
