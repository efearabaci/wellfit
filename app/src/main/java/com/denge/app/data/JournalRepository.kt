package com.denge.app.data

import android.content.Context
import android.util.AtomicFile
import androidx.room.withTransaction
import com.denge.app.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import java.io.File
import java.time.Instant
import java.time.LocalDate

class JournalRepository(private val context: Context, private val db: DengeDatabase,
    private val recoveryFile: File = File(context.noBackupFilesDir, "journal-recovery.json")) {
    private val mutex = Mutex()
    private fun decode(entity: JournalEntity?): AppState {
        if (entity != null) return JsonCodec.decodeFromString(entity.json)
        check(!recoveryFile.exists()) { "Günlük bulunamadı; kurtarma kopyan korunuyor. Kurtarma ekranını kullan." }
        return AppState()
    }
    val states = db.journal().observe().map { decode(it) }.flowOn(Dispatchers.IO)
    suspend fun read(): AppState = withContext(Dispatchers.IO) { decode(db.journal().read()) }
    suspend fun recover(verified: AppState): AppState = mutex.withLock {
        validateState(verified)
        withContext(Dispatchers.IO) {
            db.journal().read()?.let { existing ->
                File(context.noBackupFilesDir, "journal-before-recovery-${System.currentTimeMillis()}.json").writeText(existing.json)
            }
        }
        db.journal().put(JournalEntity(json = JsonCodec.encodeToString(verified)))
        verified
    }
    suspend fun update(transform: (AppState) -> AppState): AppState = mutex.withLock {
        val next = db.withTransaction {
            val old = read()
            val new = transform(old).copy(revision = old.revision + 1)
            validateState(new)
            db.journal().put(JournalEntity(json = JsonCodec.encodeToString(new)))
            new
        }
        // A separate atomic recovery copy, outside the SQLite file. Failure cannot undo a commit.
        withContext(Dispatchers.IO) {
            runCatching {
                val file = AtomicFile(recoveryFile)
                val stream = file.startWrite()
                try { stream.write(JsonCodec.encodeToString(next).toByteArray()); file.finishWrite(stream) }
                catch (e: Exception) { file.failWrite(stream); throw e }
            }
        }
        next
    }
    suspend fun edit(date: LocalDate, transform: (Day) -> Day) = update { it.changeDay(date.toString(), transform) }
    suspend fun close(date: LocalDate): AppState = update { state ->
        val d = state.day(date)
        require(!d.closed) { "Gün zaten kapalı." }
        val closed = d.copy(closed = true, closedAt = Instant.now().toString())
        val snapshot = state.copy(days = state.days + (d.date to closed))
        // Preserve the closed day even when the API context exceeds the limit.
        val prompt = runCatching { MemoryBuilder.payload(snapshot, date) }
        val recap = Recap(createdAt = Instant.now().toString(), model = state.profile.model,
            snapshotGzip = SnapshotCodec.pack(prompt.getOrDefault("")), status = if (prompt.isSuccess) "pending" else "failed", error = prompt.exceptionOrNull()?.message.orEmpty())
        snapshot.copy(days = snapshot.days + (d.date to closed.copy(recap = recap)))
    }
    suspend fun reopen(date: LocalDate) = update { state ->
        val d = state.day(date)
        state.copy(days = state.days + (d.date to d.copy(closed = false, closedAt = null, recap = null,
            archivedRecaps = d.archivedRecaps + listOfNotNull(d.recap))))
    }
    suspend fun recapStatus(date: String, id: String, status: String, text: String = "", error: String = "") = update { state ->
        val d = state.days[date]
        if (d?.recap?.id != id || !d.closed) state else state.copy(days = state.days + (date to d.copy(recap = d.recap.copy(status = status, text = text, error = error))))
    }
}
