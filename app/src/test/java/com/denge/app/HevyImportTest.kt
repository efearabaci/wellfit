package com.denge.app

import com.denge.app.data.BackupCodec
import com.denge.app.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class HevyImportTest {
    private val now = "2026-10-19T18:00:00Z"
    private val today = LocalDate.of(2026,10,19)
    private fun record(date: String = "2026-10-18", id: String = "one") = HevyRecord(LocalDate.parse(date), Workout(id = "hevy:$id", title = "Full Body", minutes = 50, source = "Hevy", rawJson = "{\"sets\":[1,2,3]}"))
    @Test fun `history import preserves local meals notes and manual workouts and is idempotent`() {
        val manual = Workout(id = "manual", title = "Yürüyüş", minutes = 20)
        val date = "2026-10-18"
        val base = AppState(days = mapOf(date to Day(date, note = "Uyku 7 saat", weight = 80.0, workouts = listOf(manual))))
        val first = HevyImport.merge(base, listOf(record()), now, today)
        val second = HevyImport.merge(first.state, listOf(record(), record()), now, today)
        val day = second.state.days.getValue(date)
        assertEquals(2, day.workouts.size); assertEquals("Uyku 7 saat", day.note); assertEquals(80.0, day.weight!!, .01)
        assertEquals(1, second.importedWorkouts); assertEquals("trained", day.workoutStatus)
        assertEquals(first.state, second.state)
    }
    @Test fun `closed days and their exact recap snapshots remain unchanged`() {
        val date = "2026-10-18"
        val closed = Day(date, closed = true, recap = Recap(createdAt = now, snapshotGzip = SnapshotCodec.pack("{}"), model = "test"))
        val result = HevyImport.merge(AppState(days = mapOf(date to closed)), listOf(record()), now, today)
        assertEquals(closed, result.state.days[date]); assertEquals(1, result.skippedClosedDays); assertEquals(0, result.importedWorkouts)
    }
    @Test fun `new training dates are added without inventing rest or nutrition records`() {
        val result = HevyImport.merge(AppState(), listOf(record()), now, today)
        assertEquals(1, result.state.days.size); assertTrue(result.state.days.values.single().meals.isEmpty()); assertFalse(result.state.days.values.single().closed)
    }
    @Test fun `changed open workout date is moved without duplicate attendance`() {
        val original = record()
        val base = AppState(days = mapOf("2026-10-18" to Day("2026-10-18", workouts = listOf(original.workout), workoutStatus = "trained")))
        val result = HevyImport.merge(base, listOf(record("2026-10-19")), now, today)
        assertTrue(result.state.days.getValue("2026-10-18").workouts.isEmpty())
        assertEquals("unknown", result.state.days.getValue("2026-10-18").workoutStatus)
        assertEquals(1, PeriodStatistics(result.state, StatsPeriod(PeriodMode.MONTH, today, today)).trainingDays.size)
    }
    @Test fun `closed source ids are never copied into a second date`() {
        val base = AppState(days = mapOf("2026-10-18" to Day("2026-10-18", workouts = listOf(record().workout), closed = true)))
        val result = HevyImport.merge(base, listOf(record("2026-10-19")), now, today)
        assertFalse(result.state.days.containsKey("2026-10-19")); assertEquals(1, result.skippedClosedDays)
    }
    @Test fun `future workouts fail the entire import`() {
        assertThrows(IllegalArgumentException::class.java) { HevyImport.merge(AppState(), listOf(record("2026-10-20")), now, today) }
    }
    @Test fun `new connection metadata and raw workouts survive backup round trip`() {
        val s = HevyImport.merge(AppState(hevy = HevyConnection(verifiedAt = now, workoutsOnAccount = 1)), listOf(record()), now, today).state
        assertEquals(s, BackupCodec.decode(BackupCodec.encode(s)))
    }
    @Test fun `version one backups remain readable after upgrade`() {
        val oldJson = """{"schema":1,"profile":{"onboarded":true},"days":{}}"""
        val state = JsonCodec.decodeFromString<AppState>(oldJson)
        assertNull(state.hevy.verifiedAt); assertTrue(state.profile.onboarded)
    }
}
