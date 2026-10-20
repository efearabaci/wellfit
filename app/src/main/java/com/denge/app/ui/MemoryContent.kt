package com.denge.app.ui

import java.util.Locale

internal enum class MemoryTone { DAY, WEEK, MONTH, NEXT }
internal data class MemoryBlock(val text: String, val marker: String? = null)
internal data class MemoryArticle(val title: String, val tone: MemoryTone, val blocks: List<MemoryBlock>)

/** Groups existing AI output for reading; no requests or changes to the stored assessment. */
internal fun memoryArticles(source: String): List<MemoryArticle> {
    val locale = Locale.forLanguageTag("tr-TR")
    fun knownHeading(raw: String): Pair<String, MemoryTone>? {
        val title = raw.trim().trimEnd(':').trim().lowercase(locale)
        return when (title) {
            "bugünün özeti", "günün özeti", "günlük özet", "gün değerlendirmesi" -> "Gün değerlendirmesi" to MemoryTone.DAY
            "bu hafta", "haftalık özet", "haftalık değerlendirme" -> "Bu hafta" to MemoryTone.WEEK
            "bu ay", "aylık özet", "aylık değerlendirme" -> "Bu ay" to MemoryTone.MONTH
            "sonraki gün için öneriler", "yarın için öneriler", "yarın", "sonraki gün" -> "Sonraki gün" to MemoryTone.NEXT
            else -> null
        }
    }
    val result = mutableListOf<MemoryArticle>()
    var title = "Değerlendirme"
    var tone = MemoryTone.DAY
    val blocks = mutableListOf<MemoryBlock>()
    val paragraph = mutableListOf<String>()
    fun flushParagraph() {
        if (paragraph.isNotEmpty()) { blocks += MemoryBlock(paragraph.joinToString("\n")); paragraph.clear() }
    }
    fun flushSection() {
        flushParagraph()
        if (blocks.isNotEmpty()) result += MemoryArticle(title, tone, blocks.toList())
        blocks.clear()
    }
    for (raw in source.lineSequence()) {
        val line = raw.trim()
        if (line.isEmpty() || line.matches(Regex("([-*_])\\1{2,}"))) { flushParagraph(); continue }
        val withoutNumber = line.replaceFirst(Regex("^\\d+[.)]\\s+"), "")
        val headingMatch = Regex("^#{1,6}\\s+(.+?)(?:\\s+#+)?$").matchEntire(line)
        val candidate = (headingMatch?.groupValues?.get(1) ?: withoutNumber).replace("**", "").replace("__", "").trim()
        val known = knownHeading(candidate)
        if (headingMatch != null || known != null) {
            flushSection()
            title = known?.first ?: candidate.trimEnd(':')
            tone = known?.second ?: MemoryTone.DAY
            continue
        }
        // Models occasionally put a paragraph on the same line as a required heading.
        val inlineHeading = Regex("^(?:\\d+[.)]\\s+)?(?:\\*\\*|__)?([^:]+?)(?:\\*\\*|__)?:\\s*(?:\\*\\*|__)?\\s*(.+)$").matchEntire(line)
        val inlineKnown = inlineHeading?.let { knownHeading(it.groupValues[1]) }
        if (inlineHeading != null && inlineKnown != null) {
            flushSection(); title = inlineKnown.first; tone = inlineKnown.second
            paragraph += inlineHeading.groupValues[2]; continue
        }
        val bullet = Regex("^([-*•]|\\d+[.)])\\s+(.+)$").matchEntire(line)
        if (bullet != null) {
            flushParagraph()
            blocks += MemoryBlock(bullet.groupValues[2], if (bullet.groupValues[1].first().isDigit()) bullet.groupValues[1] else "•")
        } else if (raw.startsWith("  ") && blocks.lastOrNull()?.marker != null && paragraph.isEmpty()) {
            val last = blocks.removeAt(blocks.lastIndex)
            blocks += last.copy(text = "${last.text} $line")
        } else paragraph += line
    }
    flushSection()
    // An unusual heading-only answer still remains readable instead of becoming an empty card.
    if (result.isEmpty() && source.isNotBlank()) result += MemoryArticle("Değerlendirme", MemoryTone.DAY, listOf(MemoryBlock(source.trim())))
    return result
}
