package com.denge.app.data

import android.content.Context
import android.net.Uri
import com.denge.app.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonObject
import java.security.MessageDigest

@Serializable data class BackupEnvelope(val format: String = "denge-backup-v1", val sha256: String, val data: String)
object BackupCodec {
    private fun hash(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    fun encode(state: AppState): String {
        val data = JsonCodec.encodeToString(state)
        return JsonCodec.encodeToString(BackupEnvelope(sha256 = hash(data), data = data))
    }
    fun decode(raw: String): AppState {
        val envelope = JsonCodec.decodeFromString<BackupEnvelope>(raw)
        require(envelope.format == "denge-backup-v1" && envelope.sha256 == hash(envelope.data)) { "Yedek doğrulanamadı. Dosya eksik veya değişmiş olabilir." }
        return JsonCodec.decodeFromString<AppState>(envelope.data).also { state ->
            validateState(state)
            state.days.values.forEach { day ->
                (day.archivedRecaps + listOfNotNull(day.recap)).forEach { recap ->
                    val prompt = recap.prompt
                    if (prompt.isNotBlank()) JsonCodec.parseToJsonElement(prompt).jsonObject
                }
            }
        }
    }
}
class BackupService(private val context: Context) {
    suspend fun export(uri: Uri, state: AppState, markdown: Boolean = false) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use {
            it.write(if (markdown) MemoryBuilder.markdown(state) else BackupCodec.encode(state))
        } ?: error("Yedek dosyası açılamadı.")
    }
    suspend fun read(uri: Uri): AppState = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count == -1) break
                require(output.size() + count <= 30_000_000) { "Yedek 30 MB sınırını aşıyor." }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } ?: error("Yedek okunamadı.")
        require(bytes.size <= 30_000_000) { "Yedek 30 MB sınırını aşıyor." }
        BackupCodec.decode(bytes.toString(Charsets.UTF_8))
    }
    suspend fun recovery(): AppState = withContext(Dispatchers.IO) {
        val raw = android.util.AtomicFile(java.io.File(context.noBackupFilesDir, "journal-recovery.json")).readFully().toString(Charsets.UTF_8)
        JsonCodec.decodeFromString<AppState>(raw).also { validateState(it) }
    }
}
