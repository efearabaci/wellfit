package com.denge.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.denge.app.DengeApplication
import com.denge.app.domain.*
import com.denge.app.integration.RecapWorker
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.*

class DengeViewModel(app: Application) : AndroidViewModel(app) {
    val graph = (app as DengeApplication).graph
    val state = MutableStateFlow<AppState?>(null)
    val loadError = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    val events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val keysRevision = MutableStateFlow(0)
    init { observe() }
    private fun observe() {
        viewModelScope.launch {
            try {
                var recovered = false
                graph.repo.states.collect { s ->
                    state.value = s
                    if (!recovered) { RecapWorker.recover(getApplication(), s); recovered = true }
                }
            } catch (e: Exception) { loadError.value = "Kayıtlar okunamadı. Veriler silinmedi. Yedekten kurtarmayı deneyebilirsin." }
        }
    }
    fun action(success: String? = null, done: () -> Unit = {}, block: suspend () -> Unit) {
        if (busy.value) return
        viewModelScope.launch {
            busy.value = true
            try { block(); done(); success?.let { events.emit(it) } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { events.emit(e.message ?: "İşlem tamamlanamadı. Tekrar deneyebilirsin.") }
            finally { busy.value = false }
        }
    }
    fun meal(date: LocalDate, value: Meal, done: () -> Unit) = action("Öğün kaydedildi", done) {
        value.validate(); graph.repo.edit(date) { d -> d.copy(meals = d.meals.filterNot { it.id == value.id } + value) }
    }
    fun deleteMeal(date: LocalDate, id: String, done: () -> Unit) = action("Öğün silindi", done) { graph.repo.edit(date) { it.copy(meals = it.meals.filterNot { m -> m.id == id }) } }
    fun metrics(date: LocalDate, steps: Int?, weight: Double?, done: () -> Unit) = action("Güncellendi", done) {
        require(steps == null || steps in 0..200000) { "Adım 0–200.000 arasında olmalı." }
        require(weight == null || (weight.isFinite() && weight in 20.0..500.0)) { "Kiloyu kontrol et (20–500 kg)." }
        graph.repo.edit(date) {
            val stepsChanged = it.steps != steps
            it.copy(steps = steps, stepSource = if (stepsChanged) "Manuel" else it.stepSource,
                stepSyncedAt = if (stepsChanged) null else it.stepSyncedAt, weight = weight)
        }
    }
    fun note(date: LocalDate, value: String, done: () -> Unit) = action("Not kaydedildi", done) { graph.repo.edit(date) { it.copy(note = value) } }
    fun workout(date: LocalDate, value: Workout, done: () -> Unit) = action("Antrenman kaydedildi", done) {
        require(value.title.isNotBlank() && value.minutes in 1..1440) { "Antrenman adı ve geçerli bir süre gir." }
        graph.repo.edit(date) { d -> d.copy(workoutStatus = "trained", workouts = d.workouts.filterNot { it.id == value.id } + value) }
    }
    fun deleteWorkout(date: LocalDate, id: String, done: () -> Unit) = action("Antrenman silindi", done) { graph.repo.edit(date) { d ->
        val remaining = d.workouts.filterNot { it.id == id }
        d.copy(workouts = remaining, workoutStatus = if (remaining.isNotEmpty()) "trained" else "unknown")
    } }
    fun rest(date: LocalDate) = action("Dinlenme günü kaydedildi") { graph.repo.edit(date) { require(it.workouts.isEmpty()) { "Önce kayıtlı antrenmanları kaldır." }; it.copy(workoutStatus = "rest") } }
    fun hevy(date: LocalDate) = action {
        require(graph.secrets.has("hevy")) { "Önce Ayarlar'dan Hevy API anahtarını ekle. Hevy Pro gerekir." }
        val d = graph.repo.read().day(date)
        val workouts = graph.remote.workouts(graph.secrets.get("hevy"), date, ZoneId.of(d.zone))
        graph.repo.edit(date) { it.copy(workouts = it.workouts.filterNot { w -> w.source == "Hevy" } + workouts,
            workoutStatus = if (workouts.isNotEmpty() || it.workouts.any { w -> w.source != "Hevy" }) "trained" else "unknown") }
        events.emit(if (workouts.isEmpty()) "Hevy'de bu güne ait antrenman bulunamadı." else "${workouts.size} antrenman, tüm hareket ve setleriyle eşitlendi.")
    }
    fun syncSteps(date: LocalDate, explicit: Boolean = false) {
        viewModelScope.launch {
            try {
                val state = graph.repo.read(); val d = state.day(date)
                if (d.closed || (!explicit && (!state.profile.healthEnabled || (d.steps != null && d.stepSource == "Manuel")))) return@launch
                if (!graph.steps.available() || !graph.steps.granted()) {
                    if (explicit) events.emit("Health Connect kullanılamıyor veya adım izni verilmedi.")
                    return@launch
                }
                val steps = graph.steps.read(date, ZoneId.of(d.zone))
                if (steps != null) graph.repo.update { s ->
                    val latest = s.day(date)
                    if (latest.closed || (!explicit && latest.steps != null && latest.stepSource == "Manuel")) s
                    else s.changeDay(d.date) { it.copy(steps = steps, stepSource = "Health Connect", stepSyncedAt = Instant.now().toString()) }
                }
                if (explicit) events.emit(if (steps == null) "Bu gün için adım verisi bulunamadı. Manuel kayıt korunuyor." else "Adımlar güncellendi.")
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (explicit) events.emit("Adımlar okunamadı. Manuel giriş yapabilirsin.") }
        }
    }
    fun healthGranted() = action("Health Connect bağlandı") { graph.repo.update { it.copy(profile = it.profile.copy(healthEnabled = true)) } }
    fun close(date: LocalDate, done: () -> Unit) = action("Gün kapatıldı. Kayıtların güvende.", done) {
        val before = graph.repo.read()
        val day = before.day(date)
        if (before.profile.healthEnabled && (day.steps == null || day.stepSource != "Manuel")) {
            try {
                if (graph.steps.granted()) {
                    graph.steps.read(date, ZoneId.of(day.zone))?.let { steps ->
                        graph.repo.edit(date) { it.copy(steps = steps, stepSource = "Health Connect", stepSyncedAt = Instant.now().toString()) }
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { events.emit("Son adım eşitlemesi yapılamadı; gün, kayıtlı adımlarınla kapatılıyor.") }
        }
        val s = graph.repo.close(date)
        s.days[date.toString()]?.recap?.let { if (it.status == "pending") RecapWorker.enqueue(getApplication(), date.toString(), it.id) }
    }
    fun reopen(date: LocalDate, done: () -> Unit) = action("Gün yeniden açıldı", done) {
        graph.repo.read().day(date).recap?.let { WorkManager.getInstance(getApplication<Application>()).cancelUniqueWork("recap:${it.id}") }
        graph.repo.reopen(date)
    }
    fun retry(date: LocalDate) = action("Özet isteği sıraya alındı") {
        val s = graph.repo.read()
        require(s.profile.aiConsent && graph.secrets.has("deepseek")) { "Ayarlar'dan DeepSeek anahtarını ekle ve AI paylaşımını etkinleştir." }
        val d = s.day(date); require(d.closed)
        val old = d.recap ?: error("Özet kaydı bulunamadı.")
        require(old.prompt.isNotBlank()) { old.error }
        graph.repo.recapStatus(date.toString(), old.id, "pending")
        RecapWorker.enqueue(getApplication(), date.toString(), old.id)
    }
    fun profile(profile: Profile, deepseek: String, hevy: String, done: () -> Unit = {}) = action("Ayarlar kaydedildi", done) {
        profile.goals.validate()
        if (deepseek.isNotBlank()) graph.secrets.set("deepseek", deepseek)
        if (hevy.isNotBlank()) graph.secrets.set("hevy", hevy)
        val s = graph.repo.update { old ->
            old.copy(profile = profile, days = old.days.mapValues { (_, d) ->
                if (!d.closed && d.date == LocalDate.now().toString()) d.copy(goals = profile.goals) else d
            })
        }
        keysRevision.value++
        if (!profile.aiConsent) s.days.values.forEach { d -> d.recap?.let { WorkManager.getInstance(getApplication<Application>()).cancelUniqueWork("recap:${it.id}") } }
        else RecapWorker.recover(getApplication(), s)
    }
    fun removeKey(name: String) = action("Anahtar kaldırıldı") { graph.secrets.set(name, ""); keysRevision.value++ }
    fun export(uri: Uri, markdown: Boolean = false) = action(if (markdown) "Hafıza dışa aktarıldı" else "Yedek kaydedildi") {
        val s = graph.repo.read()
        graph.backup.export(uri, s.copy(lastExportAt = Instant.now().toString()), markdown)
        if (!markdown) graph.repo.update { it.copy(lastExportAt = Instant.now().toString()) }
    }
    fun readBackup(uri: Uri, ready: (AppState) -> Unit) = action { ready(graph.backup.read(uri)) }
    fun restore(s: AppState, done: () -> Unit) = action("Yedek birleştirildi", done) {
        if (loadError.value != null) {
            state.value = graph.repo.recover(s.copy(profile = s.profile.copy(aiConsent = false, healthEnabled = false)))
            loadError.value = null
            observe()
            return@action
        }
        // Import never overwrites a local day: conflicts must be resolved by retaining the local record.
        graph.repo.update { old ->
            s.copy(profile = if (old.profile.onboarded) old.profile else s.profile.copy(aiConsent = false, healthEnabled = false), days = s.days + old.days)
        }.also { RecapWorker.recover(getApplication(), it) }
    }
    fun recoverLocal() = action("Kurtarma kopyası doğrulandı ve geri yüklendi.") {
        val s = graph.backup.recovery()
        state.value = graph.repo.recover(s)
        loadError.value = null
        observe()
    }
}
