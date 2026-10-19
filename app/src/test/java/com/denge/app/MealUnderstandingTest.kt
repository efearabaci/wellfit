package com.denge.app

import com.denge.app.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class MealUnderstandingTest {
    private val date = LocalDate.of(2026, 10, 19)
    private fun meal(carbs: Double? = null, fat: Double? = null) = Meal(name = "Tavuk", category = "Öğle", time = "12:30", calories = 300, protein = 40.0, carbs = carbs, fat = fat)
    @Test fun `optional macros survive serialization without turning into zeros`() {
        val original = meal(); original.validate()
        val restored = JsonCodec.decodeFromString<Meal>(JsonCodec.encodeToString(Meal.serializer(), original))
        assertNull(restored.carbs); assertNull(restored.fat)
        assertEquals(0.0, meal(0.0, 0.0).carbs!!, 0.0)
    }
    @Test fun `incomplete daily macro totals and period averages remain unknown`() {
        val incomplete = Day(date.toString(), closed = true, meals = listOf(meal(), meal(25.0, 3.0)))
        val known = Day(date.minusDays(1).toString(), closed = true, meals = listOf(meal(30.0, 8.0)))
        assertNull(incomplete.carbs); assertNull(incomplete.fat)
        assertEquals(600, incomplete.calories); assertEquals(80.0, incomplete.protein, 0.0)
        val stats = PeriodStatistics(AppState(days = mapOf(incomplete.date to incomplete, known.date to known)), StatsPeriod(PeriodMode.MONTH, date, date))
        assertEquals(30.0, stats.carbsAverage!!, 0.0); assertEquals(8.0, stats.fatAverage!!, 0.0)
        assertEquals(1, stats.knownCarbDays.size); assertEquals(2, stats.closedDays.size)
    }
    @Test fun `old fully specified meal records retain their values`() {
        val restored = JsonCodec.decodeFromString<Meal>("""{"name":"Öğün","category":"Öğle","time":"12:30","calories":500,"protein":30.0,"carbs":60.0,"fat":12.0}""")
        restored.validate(); assertEquals(60.0, restored.carbs!!, 0.0); assertNull(restored.aiSourceText)
    }
    @Test fun `memory includes missing macros and original AI source and assumptions`() {
        val m = meal().copy(aiSourceText = "bir porsiyon tavuk", aiNote = "150 g varsayıldı")
        val day = Day(date.toString(), closed = true, meals = listOf(m))
        val state = AppState(days = mapOf(day.date to day))
        val prompt = MemoryBuilder.payload(state, date)
        assertTrue(prompt.contains("\"carbsG\":null")); assertTrue(prompt.contains("bir porsiyon tavuk")); assertTrue(prompt.contains("150 g varsayıldı"))
        assertTrue(MemoryBuilder.markdown(state).contains("Bilinmiyor"))
    }
    @Test fun `AI meals preserve provenance and have distinct local identifiers`() {
        val food = UnderstoodFood("Yumurta", "Kahvaltı", "09:00", 150, 12.0, null, 10.0, "2 yumurta varsayıldı")
        val results = UnderstoodMeals(listOf(food, food.copy(name = "Yoğurt")), "Kontrol et").toMeals("sabah yumurta ve yoğurt")
        assertEquals(2, results.map { it.id }.distinct().size)
        assertEquals("sabah yumurta ve yoğurt", results.first().aiSourceText); assertNull(results.first().carbs)
    }
    @Test fun `invalid AI item rejects the entire batch`() {
        val food = UnderstoodFood("Yumurta", "Kahvaltı", "09:00", 150, 12.0, null, 10.0, "")
        listOf(food.copy(calories = -1), food.copy(protein = Double.NaN), food.copy(category = "Dinner"), food.copy(time = "25:00"), food.copy(carbs = -3.0)).forEach { invalid ->
            assertTrue(runCatching { UnderstoodMeals(listOf(food, invalid), "").toMeals("test") }.isFailure)
        }
    }
    @Test fun `empty clarification response creates no meals`() {
        assertTrue(UnderstoodMeals(emptyList(), "Ne kadar yedin?").toMeals("bir şeyler yedim").isEmpty())
    }
}
