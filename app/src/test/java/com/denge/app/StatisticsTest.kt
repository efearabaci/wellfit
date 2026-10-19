package com.denge.app

import com.denge.app.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class StatisticsTest {
    private val today = LocalDate.of(2026, 10, 19)
    private fun day(date: String, closed: Boolean = false, calories: Int? = null, steps: Int? = null, weight: Double? = null, workouts: List<Workout> = emptyList(), status: String = "unknown", goals: Goals = Goals()) = Day(date, goals = goals,
        closed = closed, steps = steps, weight = weight, workouts = workouts, workoutStatus = status,
        meals = calories?.let { listOf(Meal(name = "Test öğünü", category = "Öğle", time = "12:00", calories = it, protein = 100.0, carbs = 100.0, fat = 20.0)) }.orEmpty())
    private fun stats(vararg days: Day, mode: PeriodMode = PeriodMode.MONTH) = PeriodStatistics(AppState(days = days.associateBy { it.date }), StatsPeriod(mode, today, today))
    @Test fun `weeks begin Monday cross years and navigate without losing calendar boundaries`() {
        val p = StatsPeriod(PeriodMode.WEEK, LocalDate.of(2027,1,1), LocalDate.of(2027,1,1))
        assertEquals(LocalDate.of(2026,12,28), p.start); assertEquals(LocalDate.of(2027,1,3), p.end)
        assertEquals(5, p.elapsedDates.size); assertFalse(p.canAdvance)
        assertEquals(LocalDate.of(2026,12,21), p.shift(-1).start)
    }
    @Test fun `months support leap years and never count future days`() {
        val leap = StatsPeriod(PeriodMode.MONTH, LocalDate.of(2024,2,29), LocalDate.of(2024,2,29))
        assertEquals(29, leap.dates.size)
        val current = StatsPeriod(PeriodMode.MONTH, today, today)
        assertEquals(31, current.dates.size); assertEquals(19, current.elapsedDates.size)
        assertEquals(LocalDate.of(2026,9,1), current.shift(-1).start); assertFalse(current.canAdvance)
    }
    @Test fun `missing and open nutrition days do not distort closed day averages`() {
        val s = stats(day("2026-10-01", true, 1800), day("2026-10-02", false, 200), day("2026-10-03", true, 2200))
        assertEquals(2000.0, s.caloriesAverage!!, .01); assertEquals(4000, s.caloriesTotal.toInt())
        assertEquals(2, s.closedDays.size); assertEquals(3, s.meals.size)
        assertEquals(100.0, s.proteinAverage!!, .01)
    }
    @Test fun `unknown steps remain unknown but recorded zero is a real measurement`() {
        val s = stats(day("2026-10-01", steps = null), day("2026-10-02", steps = 0), day("2026-10-03", steps = 10000))
        assertEquals(2, s.stepDays.size); assertEquals(5000.0, s.stepsAverage!!, .01)
        assertEquals(1, s.stepTargetDays.reached); assertEquals(2, s.stepTargetDays.eligible)
        assertEquals("2026-10-03", s.bestStepDay!!.date)
    }
    @Test fun `two sessions on one day count as one training day`() {
        val s = stats(day("2026-10-05", workouts = listOf(Workout(id = "a", title = "Sabah", minutes = 30), Workout(id = "b", title = "Akşam", minutes = 45))), day("2026-10-06", status = "rest"))
        assertEquals(1, s.trainingDays.size); assertEquals(2, s.workoutCount); assertEquals(75, s.trainingMinutes.toInt())
        assertEquals(37.5, s.averageSessionMinutes!!, .01); assertEquals(1, s.restDays.size)
        assertEquals(17, s.unknownActivityDays)
        assertEquals(1, s.weekdayActivity.first { it.weekday == DayOfWeek.MONDAY }.trainingDays)
    }
    @Test fun `explicit rest is data while absent workouts are not assumed rest`() {
        val rest = day("2026-10-01", status = "rest")
        assertTrue(rest.hasData)
        assertEquals(ActivityMark.REST, activityMark(rest, LocalDate.parse(rest.date), today))
        assertEquals(ActivityMark.UNKNOWN, activityMark(null, today, today))
        assertEquals(ActivityMark.FUTURE, activityMark(null, today.plusDays(1), today))
    }
    @Test fun `historic goals are used instead of current profile goals`() {
        val s = stats(day("2026-10-01", true, 1800, 6500, goals = Goals(calories = 1800, protein = 90, steps = 6000)),
            day("2026-10-02", true, 2200, 6500, goals = Goals(calories = 2200, protein = 140, steps = 8000)))
        assertEquals(2, s.calorieTargetDays.reached); assertEquals(1, s.proteinTargetDays.reached); assertEquals(1, s.stepTargetDays.reached)
    }
    @Test fun `disabled protein goals are not counted as achieved targets`() {
        val s = stats(day("2026-10-01", true, 2000, goals = Goals(protein = 0)))
        assertEquals(0, s.proteinTargetDays.eligible)
    }
    @Test fun `single weigh in has no fabricated change and multiple readings have weekly means`() {
        val one = stats(day("2026-10-01", weight = 85.0)); assertNull(one.weightChange)
        val s = stats(day("2026-10-01", weight = 85.0), day("2026-10-03", weight = 84.0), day("2026-10-06", weight = 83.0))
        assertEquals(-2.0, s.weightChange!!, .001); assertEquals(84.0, s.averageWeight!!, .001)
        assertEquals(2, s.weeklyWeights.size); assertEquals(84.5, s.weeklyWeights.first().kg, .001)
    }
    @Test fun `future and out of period source records are excluded`() {
        val s = stats(day("2026-09-30", true, 100), day("2026-10-20", true, 9000), day("2026-10-19", true, 2000))
        assertEquals(1, s.days.size); assertEquals(2000.0, s.caloriesAverage!!, .01)
    }
    @Test fun `streaks stop on open and missing days and respect period boundaries`() {
        val s = stats(day("2026-09-30", true), day("2026-10-01", true), day("2026-10-02", true), day("2026-10-03", false), day("2026-10-04", true))
        assertEquals(2, s.longestClosedStreak)
    }
    @Test fun `empty periods return unknown averages rather than zero`() {
        val s = stats(); assertNull(s.caloriesAverage); assertNull(s.stepsAverage); assertNull(s.averageWeight); assertNull(s.averageSessionMinutes)
    }
}
