package com.denge.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.denge.app.data.*
import com.denge.app.domain.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class JournalIntegrationTest {
    private lateinit var db: DengeDatabase
    private lateinit var repo: JournalRepository
    private fun repository(database: DengeDatabase): JournalRepository {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return JournalRepository(context, database, java.io.File(context.cacheDir, "test-recovery-${java.util.UUID.randomUUID()}.json"))
    }
    @Before fun setup() { val context = ApplicationProvider.getApplicationContext<Context>(); db = Room.inMemoryDatabaseBuilder(context, DengeDatabase::class.java).build(); repo = repository(db) }
    @After fun tearDown() { db.close() }
    @Test fun closeReopenAndStaleResponseAreAtomic() = runBlocking {
        val date = LocalDate.now()
        repo.edit(date) { it.copy(meals = listOf(Meal(name = "Tavuk", category = "Öğle", time = "12:00", calories = 500, protein = 40.0, carbs = 50.0, fat = 10.0))) }
        repo.close(date)
        val first = repo.read().day(date)
        assertTrue(first.closed); assertTrue(first.recap!!.prompt.contains("Tavuk"))
        repo.reopen(date)
        repo.edit(date) { it.copy(steps = 7000) }
        repo.close(date)
        repo.recapStatus(date.toString(), first.recap.id, "ready", "Stale response")
        val current = repo.read().day(date)
        assertNotEquals(first.recap.id, current.recap!!.id); assertEquals("", current.recap.text)
        assertEquals(first.recap.id, current.archivedRecaps.single().id); assertTrue(current.recap.prompt.contains("7000"))
    }
    @Test fun simultaneousWritesDoNotLoseMeals() = runBlocking {
        val date = LocalDate.now()
        coroutineScope { (1..25).map { i -> async(Dispatchers.IO) { repo.edit(date) { d -> d.copy(meals = d.meals + Meal(name = "Öğün $i", category = "Ara öğün", time = "12:00", calories = 50, protein = 1.0, carbs = 5.0, fat = 1.0)) } } }.awaitAll() }
        assertEquals(25, repo.read().day(date).meals.size)
    }
    @Test fun keystoreAndPersistentDatabaseSurviveNewInstances() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val secret = SecretStore(context); secret.set("instrumentation-test", "test-value")
        assertEquals("test-value", SecretStore(context).get("instrumentation-test")); secret.set("instrumentation-test", "")
        val filename = "persistence-test.db"
        val disk = Room.databaseBuilder(context, DengeDatabase::class.java, filename).build()
        repository(disk).edit(LocalDate.now()) { it.copy(steps = 12345) }
        disk.close()
        val reopened = Room.databaseBuilder(context, DengeDatabase::class.java, filename).build()
        assertEquals(12345, repository(reopened).read().day(LocalDate.now()).steps)
        reopened.close(); context.deleteDatabase(filename)
        Unit
    }
    @Test fun unexpectedEmptyDatabaseDoesNotOverwriteRecovery() = runBlocking {
        val date = LocalDate.now()
        val saved = repo.edit(date) { it.copy(steps = 4567) }
        db.clearAllTables()
        try { repo.read(); fail("A missing journal must not silently become an empty state") } catch (_: IllegalStateException) { }
        repo.recover(saved)
        assertEquals(4567, repo.read().day(date).steps)
    }
}
