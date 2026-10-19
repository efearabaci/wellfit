package com.denge.app.domain

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Serializable
data class Goals(val calories: Int = 2200, val protein: Int = 140, val carbs: Int = 240, val fat: Int = 70, val steps: Int = 8000) {
    fun validate() { require(calories in 500..10000 && protein in 0..1000 && carbs in 0..1500 && fat in 0..500 && steps in 100..100000) { "Hedefleri geçerli aralıklarda gir." } }
}
@Serializable
data class Profile(val name: String = "", val goals: Goals = Goals(), val targetWeight: Double? = null, val memory: String = "", val model: String = "deepseek-flash", val aiConsent: Boolean = false, val healthEnabled: Boolean = false, val onboarded: Boolean = false)
@Serializable
data class Meal(val id: String = UUID.randomUUID().toString(), val name: String, val category: String, val time: String, val calories: Int, val protein: Double, val carbs: Double, val fat: Double) {
    fun validate() {
        require(name.isNotBlank() && name.length <= 200) { "Yemeğe bir isim ver (en fazla 200 karakter)." }
        require(calories in 0..20000 && listOf(protein, carbs, fat).all { it.isFinite() && it in 0.0..3000.0 }) { "Kalori ve makroları kontrol et." }
        java.time.LocalTime.parse(time)
    }
}
@Serializable
data class Workout(val id: String = UUID.randomUUID().toString(), val title: String, val minutes: Int, val kind: String = "Kuvvet", val notes: String = "", val source: String = "Manuel", val rawJson: String? = null)
@Serializable
data class Recap(val id: String = UUID.randomUUID().toString(), val createdAt: String, val snapshotGzip: String, val model: String, val status: String = "pending", val text: String = "", val error: String = "") {
    val prompt get() = SnapshotCodec.unpack(snapshotGzip)
}
@Serializable
data class Day(
    val date: String, val zone: String = ZoneId.systemDefault().id, val goals: Goals = Goals(),
    val meals: List<Meal> = emptyList(), val steps: Int? = null, val stepSource: String = "Manuel",
    val stepSyncedAt: String? = null, val weight: Double? = null, val workouts: List<Workout> = emptyList(),
    val workoutStatus: String = "unknown", val note: String = "", val closed: Boolean = false,
    val closedAt: String? = null, val recap: Recap? = null, val archivedRecaps: List<Recap> = emptyList()
) {
    val calories get() = meals.sumOf { it.calories }
    val protein get() = meals.sumOf { it.protein }
    val carbs get() = meals.sumOf { it.carbs }
    val fat get() = meals.sumOf { it.fat }
    val hasData get() = meals.isNotEmpty() || steps != null || weight != null || workouts.isNotEmpty() || note.isNotBlank() || closed
}
@Serializable
data class AppState(val schema: Int = 1, val revision: Long = 0, val profile: Profile = Profile(), val days: Map<String, Day> = emptyMap(), val lastExportAt: String? = null)

fun AppState.day(date: LocalDate): Day = days[date.toString()] ?: Day(date.toString(), goals = profile.goals)
fun AppState.changeDay(date: String, update: (Day) -> Day): AppState {
    val original = days[date] ?: Day(date, goals = profile.goals)
    require(!original.closed) { "Bu gün kapalı. Değiştirmek için önce yeniden aç." }
    return copy(days = days + (date to update(original)))
}
fun validateState(state: AppState) {
    require(state.schema == 1) { "Bu yedek sürümü desteklenmiyor." }
    state.profile.goals.validate()
    require(state.profile.name.length <= 100 && state.profile.memory.length <= 20000 && state.profile.model.matches(Regex("[a-zA-Z0-9._:-]{1,100}"))) { "Profil bilgileri geçersiz." }
    require(state.profile.targetWeight == null || (state.profile.targetWeight.isFinite() && state.profile.targetWeight in 20.0..500.0))
    state.days.forEach { (date, d) ->
        require(date == d.date) { "Yedekte geçersiz tarih var." }
        LocalDate.parse(date)
        ZoneId.of(d.zone); d.goals.validate()
        require(d.steps == null || d.steps in 0..200000)
        require(d.weight == null || (d.weight.isFinite() && d.weight in 20.0..500.0))
        require(d.note.length <= 10000 && d.meals.map { it.id }.distinct().size == d.meals.size)
        require(d.workouts.map { it.id }.distinct().size == d.workouts.size)
        d.meals.forEach { it.validate() }
        d.workouts.forEach { require(it.title.isNotBlank() && it.title.length <= 300 && it.minutes in 0..2880 && it.notes.length <= 20000) }
        require(d.workoutStatus in setOf("unknown", "rest", "trained"))
        (listOfNotNull(d.recap) + d.archivedRecaps).forEach { require(it.status in setOf("pending", "running", "ready", "failed")) }
    }
}
