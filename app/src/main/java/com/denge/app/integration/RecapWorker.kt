package com.denge.app.integration

import android.content.Context
import androidx.work.*
import com.denge.app.DengeApplication
import com.denge.app.domain.AppState
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.util.concurrent.TimeUnit

class RecapWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val graph = (applicationContext as DengeApplication).graph
        val date = inputData.getString("date") ?: return Result.failure()
        val id = inputData.getString("id") ?: return Result.failure()
        val state = graph.repo.read()
        val day = state.days[date] ?: return Result.success()
        val recap = day.recap ?: return Result.success()
        if (!day.closed || recap.id != id || recap.status == "ready") return Result.success()
        if (!state.profile.aiConsent || !graph.secrets.has("deepseek")) return Result.success()
        if (recap.prompt.isBlank()) return Result.failure()
        return try {
            graph.repo.recapStatus(date, id, "running")
            val text = graph.remote.recap(graph.secrets.get("deepseek"), recap.model, recap.prompt)
            // A re-opened/replaced day cannot receive the old request's result.
            graph.repo.recapStatus(date, id, "ready", text)
            Result.success()
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            val retriable = (e is IOException && (e !is ApiException || e.status == 429 || e.status >= 500)) && runAttemptCount < 4
            graph.repo.recapStatus(date, id, if (retriable) "pending" else "failed", error =
                if (retriable) "Bağlantı bekleniyor. Kayıtların güvende; özet tekrar denenecek." else e.message ?: "Özet üretilemedi.")
            if (retriable) Result.retry() else Result.failure()
        }
    }
    companion object {
        fun enqueue(context: Context, date: String, id: String) {
            val work = OneTimeWorkRequestBuilder<RecapWorker>().setInputData(workDataOf("date" to date, "id" to id))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
            WorkManager.getInstance(context).enqueueUniqueWork("recap:$id", ExistingWorkPolicy.KEEP, work)
        }
        fun recover(context: Context, state: AppState) {
            if (state.profile.aiConsent) state.days.values.filter { it.closed && it.recap?.status in listOf("pending", "running") }.forEach { d ->
                d.recap?.let { enqueue(context, d.date, it.id) }
            }
        }
    }
}
