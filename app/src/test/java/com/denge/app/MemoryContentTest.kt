package com.denge.app

import com.denge.app.ui.*
import org.junit.Assert.*
import org.junit.Test

class MemoryContentTest {
    @Test fun `four assessment sections retain their paragraphs and suggestions`() {
        val articles = memoryArticles("""
            # Bugünün özeti
            1.980 kcal ve **142 g protein** kaydedildi.

            İkinci paragraf korunur.
            ## Bu hafta
            İki antrenman kaydı var.
            **Bu ay**
            İki tartı ölçümü var.
            ### Sonraki gün için öneriler
            - Porsiyon miktarını belirt.
            - Adım kaydını güncelle.
        """.trimIndent())
        assertEquals(listOf(MemoryTone.DAY, MemoryTone.WEEK, MemoryTone.MONTH, MemoryTone.NEXT), articles.map { it.tone })
        assertEquals(2, articles.first().blocks.size)
        assertTrue(articles.first().blocks.first().text.contains("**142 g protein**"))
        assertEquals(listOf("Porsiyon miktarını belirt.", "Adım kaydını güncelle."), articles.last().blocks.map { it.text })
    }
    @Test fun `plain legacy assessments remain readable without generated headings`() {
        val text = "Eski değerlendirme.\nProtein: 120 g\n\nİkinci paragraf."
        val article = memoryArticles(text).single()
        assertEquals("Değerlendirme", article.title)
        assertEquals(listOf("Eski değerlendirme.\nProtein: 120 g", "İkinci paragraf."), article.blocks.map { it.text })
    }
    @Test fun `numbered headings do not swallow numbered suggestions`() {
        val articles = memoryArticles("1. **Bugünün özeti**\nÖlçüm var.\n2. **Sonraki gün için öneriler**\n1. Saati belirt.\n2) Miktarı belirt.\n   İkinci önerinin devamı.")
        assertEquals(2, articles.size)
        assertEquals(listOf("1.", "2)"), articles.last().blocks.map { it.marker })
        assertEquals("Miktarı belirt. İkinci önerinin devamı.", articles.last().blocks.last().text)
    }
    @Test fun `inline headings preserve the text after the colon`() {
        val articles = memoryArticles("**Bugünün özeti:** 2.000 kcal kaydedildi.\n\nBu hafta: İki gün ölçüm var.\nBu ay: Veri sınırlı.")
        assertEquals(3, articles.size)
        assertEquals("2.000 kcal kaydedildi.", articles.first().blocks.single().text)
        assertEquals("İki gün ölçüm var.", articles[1].blocks.single().text)
    }
    @Test fun `unknown headings and numeric facts are preserved`() {
        val articles = memoryArticles("# Özel değerlendirme\nKilo: 78,4 kg\n\n**Protein:** 142 g\n## Detaylar\n- Ölçüm **0 adım**.")
        assertEquals(listOf("Özel değerlendirme", "Detaylar"), articles.map { it.title })
        assertEquals("**Protein:** 142 g", articles.first().blocks.last().text)
        assertEquals("Ölçüm **0 adım**.", articles.last().blocks.single().text)
    }
    @Test fun `empty answers do not fabricate an assessment and heading only answers remain visible`() {
        assertTrue(memoryArticles("\n  ").isEmpty())
        assertEquals("# Bugünün özeti", memoryArticles("# Bugünün özeti").single().blocks.single().text)
    }
    @Test fun `line ending differences and separators do not create empty sections`() {
        val articles = memoryArticles("# Bugünün özeti\r\n\r\nMetin.\r\n---\r\n\r\n## Bu hafta\r\n\r\n## Bu ay\r\nÖlçüm var.")
        assertEquals(listOf(MemoryTone.DAY, MemoryTone.MONTH), articles.map { it.tone })
        assertEquals(listOf("Metin.", "Ölçüm var."), articles.flatMap { it.blocks }.map { it.text })
    }
}
