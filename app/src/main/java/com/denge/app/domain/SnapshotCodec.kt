package com.denge.app.domain

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** Lossless prompt storage: keeping every exact historical request need not duplicate a large JSON document on disk. */
object SnapshotCodec {
    fun pack(text: String): String {
        if (text.isBlank()) return ""
        val buffer = ByteArrayOutputStream()
        GZIPOutputStream(buffer).use { it.write(text.toByteArray(Charsets.UTF_8)) }
        return Base64.getEncoder().encodeToString(buffer.toByteArray())
    }
    fun unpack(packed: String): String {
        if (packed.isBlank()) return ""
        val bytes = Base64.getDecoder().decode(packed)
        val output = ByteArrayOutputStream()
        GZIPInputStream(ByteArrayInputStream(bytes)).use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count == -1) break
                require(output.size() + count <= MemoryBuilder.MAX_PROMPT_CHARS * 4) { "Hafıza kopyası boyut sınırını aşıyor." }
                output.write(buffer, 0, count)
            }
        }
        return output.toString(Charsets.UTF_8.name())
    }
}
