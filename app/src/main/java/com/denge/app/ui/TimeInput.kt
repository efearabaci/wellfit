package com.denge.app.ui

/**
 * Öğün saatini yazarken tolere eden biçimlendirme.
 *
 * - "1220" -> "12:20", "930" -> "9:30" (93 saat olamaz, ilk hane saat sayılır)
 * - Kullanıcı iki haneden sonra yazmaya devam ettiğinde iki nokta kendiliğinden eklenir.
 * - İki nokta kullanıcı tarafından yazıldıysa konumu korunur; yalnızca hane sınırları uygulanır.
 *
 * Değer alanı her değiştiğinde çağrılır; geçersiz saatler burada düzeltilmez, kayıt sırasında
 * [parseTimeInput] ile reddedilir.
 */
fun formatTimeInput(raw: String): String {
    if (raw.contains(':')) {
        val parts = raw.split(':')
        val hour = parts[0].filter(Char::isDigit).take(2)
        val minute = parts.getOrNull(1).orEmpty().filter(Char::isDigit).take(2)
        return if (hour.isEmpty()) minute else "$hour:$minute"
    }
    val digits = raw.filter(Char::isDigit).take(4)
    return when {
        digits.length <= 2 -> digits
        // 12:2, 12:20
        digits.length == 3 && digits.take(2).toInt() <= 23 -> "${digits.take(2)}:${digits.drop(2)}"
        // 9:30 (ilk iki hane saat olamıyor), 2:40
        digits.length == 3 -> "${digits.take(1)}:${digits.drop(1)}"
        // 1220 -> 12:20; 2400 -> 24:00 (kayıt sırasında geçersiz saat olarak bildirilir)
        else -> "${digits.take(2)}:${digits.drop(2)}"
    }
}

/** Geçerli bir saati "SS:dd" olarak döndürür; eksik veya geçersiz değerde null. */
fun parseTimeInput(raw: String): String? {
    val text = if (raw.contains(':')) raw.trim() else formatTimeInput(raw)
    val match = Regex("^(\\d{1,2}):(\\d{1,2})$").find(text) ?: return null
    val hour = match.groupValues[1].toIntOrNull() ?: return null
    val minute = match.groupValues[2].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}
