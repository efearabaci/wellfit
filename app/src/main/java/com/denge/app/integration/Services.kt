package com.denge.app.integration

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.denge.app.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.time.*
import java.util.concurrent.TimeUnit

class ApiException(val status: Int, message: String) : IOException(message)
class RemoteServices(
    private val client: OkHttpClient = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS).callTimeout(150, TimeUnit.SECONDS).build(),
    private val deepseekUrl: String = "https://api.deepseek.com/chat/completions",
    private val hevyUrl: String = "https://api.hevyapp.com"
) {
    private fun execute(request: Request): JsonObject = client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw ApiException(response.code, when (response.code) {
            401, 403 -> "API anahtarı veya erişim yetkisi geçersiz. Ayarlar bölümünü kontrol et."
            402 -> "API bakiyesi yetersiz."
            429 -> "İstek sınırına ulaşıldı. Bir süre sonra tekrar denenecek."
            else -> "Servis şu anda yanıt veremedi (HTTP ${response.code})."
        })
        // Do not put response bodies, credentials or request payloads in logs/errors.
        JsonCodec.parseToJsonElement(response.body?.string() ?: throw IOException("Servis boş yanıt verdi.")).jsonObject
    }
    suspend fun recap(key: String, model: String, prompt: String): String = withContext(Dispatchers.IO) {
        val json = buildJsonObject {
            put("model", model); put("stream", false); put("max_tokens", 2500)
            put("thinking", buildJsonObject { put("type", "disabled") })
            put("messages", buildJsonArray {
                add(buildJsonObject { put("role", "system"); put("content", MemoryBuilder.SYSTEM) })
                add(buildJsonObject { put("role", "user"); put("content", prompt) })
            })
        }
        val result = execute(Request.Builder().url(deepseekUrl).header("Authorization", "Bearer $key")
            .post(json.toString().toRequestBody("application/json".toMediaType())).build())
        val choice = result["choices"]?.jsonArray?.firstOrNull()?.jsonObject ?: throw IOException("AI yanıtı okunamadı.")
        require(choice["finish_reason"]?.jsonPrimitive?.content == "stop") { "AI yanıtı tamamlanmadı. Yeniden deneyebilirsin." }
        choice["message"]?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: throw IOException("AI boş bir özet döndürdü.")
    }
    suspend fun workouts(key: String, date: LocalDate, zone: ZoneId): List<Workout> = withContext(Dispatchers.IO) {
        val result = linkedMapOf<String, Workout>()
        var page = 1
        do {
            val json = execute(Request.Builder().url("$hevyUrl/v1/workouts?page=$page&pageSize=10").header("api-key", key).build())
            val count = json["page_count"]?.jsonPrimitive?.int ?: throw IOException("Hevy sayfalama bilgisi eksik.")
            require(count in 0..500) { "Hevy geçmişi güvenli eşitleme sınırını aştı. Hiçbir kayıt kısmen aktarılmadı." }
            val records = json["workouts"]?.jsonArray ?: throw IOException("Hevy antrenman listesi eksik.")
            records.forEach { element ->
                val w = element.jsonObject
                val start = Instant.parse(w.getValue("start_time").jsonPrimitive.content)
                if (start.atZone(zone).toLocalDate() == date) {
                    val end = Instant.parse(w.getValue("end_time").jsonPrimitive.content)
                    val exercises = w["exercises"]?.jsonArray ?: throw IOException("Hevy hareket detayları eksik.")
                    val notes = exercises.joinToString("\n\n") { exercise ->
                        val e = exercise.jsonObject
                        val sets = e["sets"]?.jsonArray.orEmpty().mapIndexed { i, set ->
                            val s = set.jsonObject
                            fun value(k: String, unit: String) = s[k]?.jsonPrimitive?.contentOrNull?.let { "$it $unit" }
                            "${i + 1}. set · " + listOfNotNull(value("weight_kg", "kg"), value("reps", "tekrar"), value("duration_seconds", "sn"), value("distance_meters", "m"), value("rpe", "RPE")).joinToString(" · ")
                        }.joinToString("\n")
                        listOfNotNull(e["title"]?.jsonPrimitive?.contentOrNull, e["notes"]?.jsonPrimitive?.contentOrNull, sets).filter { it.isNotBlank() }.joinToString("\n")
                    }
                    val hasCardio = exercises.any { e -> e.jsonObject["sets"]?.jsonArray.orEmpty().any { s ->
                        (s.jsonObject["duration_seconds"]?.jsonPrimitive?.doubleOrNull ?: 0.0) > 0 || (s.jsonObject["distance_meters"]?.jsonPrimitive?.doubleOrNull ?: 0.0) > 0
                    } }
                    val id = "hevy:" + w.getValue("id").jsonPrimitive.content
                    result[id] = Workout(id, w["title"]?.jsonPrimitive?.content ?: "Hevy antrenmanı", Duration.between(start, end).toMinutes().toInt().coerceIn(0, 2880),
                        if (hasCardio) "Kuvvet / kardiyo" else "Kuvvet", notes, "Hevy", element.toString())
                }
            }
            page++
        } while (page <= count)
        result.values.toList()
    }
}

class StepService(private val context: Context) {
    val permissions = setOf(HealthPermission.getReadPermission(StepsRecord::class))
    fun available() = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    suspend fun granted(): Boolean = available() && HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions().containsAll(permissions)
    suspend fun read(date: LocalDate, zone: ZoneId): Int? {
        require(granted()) { "Health Connect adım izni verilmedi." }
        val start = date.atStartOfDay(zone).toInstant()
        val end = minOf(date.plusDays(1).atStartOfDay(zone).toInstant(), Instant.now())
        require(start < end) { "Gelecek bir güne ait adımlar okunamaz." }
        val result = HealthConnectClient.getOrCreate(context).aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), TimeRangeFilter.between(start, end)))
        return result[StepsRecord.COUNT_TOTAL]?.toInt()
    }
}
