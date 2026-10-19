package com.denge.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs

enum class PeriodMode { WEEK, MONTH }

data class StatsPeriod(val mode: PeriodMode, val anchor: LocalDate, val today: LocalDate = LocalDate.now()) {
    val start: LocalDate = if (mode == PeriodMode.WEEK) anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) else anchor.withDayOfMonth(1)
    val end: LocalDate = if (mode == PeriodMode.WEEK) start.plusDays(6) else start.with(TemporalAdjusters.lastDayOfMonth())
    val dates: List<LocalDate> = (0..ChronoUnit.DAYS.between(start, end)).map(start::plusDays)
    val elapsedDates: List<LocalDate> = dates.filter { it <= today }
    fun shift(offset: Long) = copy(anchor = if (mode == PeriodMode.WEEK) start.plusWeeks(offset) else start.plusMonths(offset))
    val canAdvance get() = shift(1).start <= today
}

enum class ActivityMark { TRAINING, REST, UNKNOWN, FUTURE }
fun Day.trained() = workouts.isNotEmpty() || workoutStatus == "trained"
fun activityMark(day: Day?, date: LocalDate, today: LocalDate): ActivityMark = when {
    date > today -> ActivityMark.FUTURE
    day?.trained() == true -> ActivityMark.TRAINING
    day?.workoutStatus == "rest" -> ActivityMark.REST
    else -> ActivityMark.UNKNOWN
}

data class GoalScore(val reached: Int, val eligible: Int) {
    val ratio: Float get() = if (eligible == 0) 0f else reached.toFloat() / eligible
}
data class WeekdayActivity(val weekday: DayOfWeek, val trainingDays: Int, val elapsedDays: Int)
data class WeightAverage(val start: LocalDate, val kg: Double, val count: Int)

/** Every denominator is tied to observed records. Unknown days never become zero-valued days. */
class PeriodStatistics(val state: AppState, val period: StatsPeriod) {
    val days = period.elapsedDates.mapNotNull { state.days[it.toString()] }
    val recordedDays = days.filter { it.hasData }
    val closedDays = days.filter { it.closed }
    val stepDays = days.filter { it.steps != null }
    val weightDays = days.filter { it.weight != null }
    val trainingDays = days.filter { it.trained() }
    val restDays = days.filter { !it.trained() && it.workoutStatus == "rest" }
    val knownActivityDays = trainingDays.size + restDays.size
    val unknownActivityDays = period.elapsedDates.size - knownActivityDays
    // A source id may occur in an old imported journal more than once. Do not double-count sessions.
    val workouts = days.flatMap { it.workouts }.distinctBy { it.id }
    val workoutCount = workouts.size
    val trainingMinutes = workouts.sumOf { it.minutes.toLong() }
    val averageSessionMinutes: Double? = workouts.takeIf { it.isNotEmpty() }?.map { it.minutes }?.average()
    val caloriesTotal = closedDays.sumOf { it.calories.toLong() }
    val caloriesAverage: Double? = closedDays.takeIf { it.isNotEmpty() }?.map { it.calories }?.average()
    val proteinAverage: Double? = closedDays.takeIf { it.isNotEmpty() }?.map { it.protein }?.average()
    val knownCarbDays = closedDays.filter { it.carbs != null }
    val knownFatDays = closedDays.filter { it.fat != null }
    val carbsAverage: Double? = knownCarbDays.takeIf { it.isNotEmpty() }?.map { it.carbs!! }?.average()
    val fatAverage: Double? = knownFatDays.takeIf { it.isNotEmpty() }?.map { it.fat!! }?.average()
    val stepsTotal = stepDays.sumOf { it.steps!!.toLong() }
    val stepsAverage: Double? = stepDays.takeIf { it.isNotEmpty() }?.map { it.steps!! }?.average()
    val bestStepDay: Day? = stepDays.maxByOrNull { it.steps!! }
    val longestWorkout: Workout? = workouts.maxByOrNull { it.minutes }
    val calorieTargetDays = GoalScore(closedDays.count { abs(it.calories - it.goals.calories) <= it.goals.calories * .1 }, closedDays.size)
    val proteinTargetDays = closedDays.filter { it.goals.protein > 0 }.let { GoalScore(it.count { d -> d.protein >= d.goals.protein }, it.size) }
    val stepTargetDays = GoalScore(stepDays.count { it.steps!! >= it.goals.steps }, stepDays.size)
    val meals = days.flatMap { it.meals }
    val mealCategories = listOf("Kahvaltı", "Öğle", "Akşam", "Ara öğün").associateWith { c -> meals.count { it.category == c } }
    val averageMealsOnClosedDays: Double? = closedDays.takeIf { it.isNotEmpty() }?.map { it.meals.size }?.average()
    val latestWeight: Double? = weightDays.lastOrNull()?.weight
    val weightChange: Double? = if (weightDays.size >= 2) weightDays.last().weight!! - weightDays.first().weight!! else null
    val averageWeight: Double? = weightDays.takeIf { it.isNotEmpty() }?.map { it.weight!! }?.average()
    val minimumWeight: Double? = weightDays.minOfOrNull { it.weight!! }
    val maximumWeight: Double? = weightDays.maxOfOrNull { it.weight!! }
    val weekdayActivity = DayOfWeek.values().map { weekday -> WeekdayActivity(weekday,
        trainingDays.count { LocalDate.parse(it.date).dayOfWeek == weekday }, period.elapsedDates.count { it.dayOfWeek == weekday }) }
    val weeklyWeights = weightDays.groupBy { LocalDate.parse(it.date).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
        .map { (date, records) -> WeightAverage(date, records.map { it.weight!! }.average(), records.size) }.sortedBy { it.start }
    val longestClosedStreak: Int = run {
        var current = 0; var longest = 0
        period.elapsedDates.forEach { d -> current = if (state.days[d.toString()]?.closed == true) current + 1 else 0; longest = maxOf(longest, current) }
        longest
    }
}
