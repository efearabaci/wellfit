package com.denge.app.domain

import java.time.Instant
import java.time.LocalDate

/** Source records already have a local date derived from their start instant at fetch time. */
data class HevyRecord(val date: LocalDate, val workout: Workout)
data class HevyImportResult(val state: AppState, val importedWorkouts: Int, val updatedDays: Int, val skippedClosedDays: Int)

object HevyImport {
    fun merge(state: AppState, records: List<HevyRecord>, syncedAt: String, today: LocalDate = LocalDate.now()): HevyImportResult {
        Instant.parse(syncedAt)
        val unique = records.distinctBy { it.workout.id }
        require(unique.all { it.workout.source == "Hevy" && it.workout.id.startsWith("hevy:") }) { "Hevy kaynak kaydı geçersiz." }
        require(unique.none { it.date > today }) { "Hevy gelecekte başlayan bir antrenman döndürdü. Aktarım yapılmadı." }
        val closedOwners = state.days.values.filter { it.closed }.flatMap { d -> d.workouts.map { it.id to d.date } }.toMap()
        val skippedDates = unique.mapNotNull { r -> if (state.days[r.date.toString()]?.closed == true) r.date.toString() else closedOwners[r.workout.id] }.toSet()
        val eligible = unique.filter { state.days[it.date.toString()]?.closed != true && it.workout.id !in closedOwners }
        val grouped = eligible.groupBy { it.date.toString() }
        val incomingOwners = eligible.associate { it.workout.id to it.date.toString() }
        // If a start date was corrected in Hevy, move the open source record rather than duplicate it.
        val cleaned = state.days.mapValues { (date, day) ->
            if (day.closed) day else {
                val remaining = day.workouts.filterNot { w -> incomingOwners[w.id]?.let { it != date } == true }
                if (remaining == day.workouts) day else day.copy(workouts = remaining, workoutStatus = if (remaining.isEmpty()) "unknown" else "trained")
            }
        }
        val updates = grouped.mapValues { (date, values) ->
            val old = cleaned[date] ?: Day(date = date, goals = state.profile.goals)
            old.copy(workouts = old.workouts.filterNot { it.source == "Hevy" } + values.map { it.workout }, workoutStatus = "trained")
        }
        val next = state.copy(days = cleaned + updates, hevy = state.hevy.copy(syncedAt = syncedAt))
        validateState(next)
        return HevyImportResult(next, eligible.size, updates.size, skippedDates.size)
    }
}
