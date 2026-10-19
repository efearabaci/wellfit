package com.denge.app

import com.denge.app.integration.*
import com.denge.app.domain.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class RemoteServicesTest {
    @Test fun `Hevy paginates fully filters by local day and preserves all set details`() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse().setBody("""{"page_count":2,"workouts":[{"id":"old","title":"Eski","start_time":"2026-10-18T15:00:00Z","end_time":"2026-10-18T16:00:00Z","exercises":[]}]}"""))
            server.enqueue(MockResponse().setBody("""{"page_count":2,"workouts":[{"id":"new","title":"Gece","start_time":"2026-10-18T22:00:00Z","end_time":"2026-10-18T23:00:00Z","exercises":[{"title":"Koşu","sets":[{"duration_seconds":600,"distance_meters":2000,"reps":null,"weight_kg":null}]}]}]}"""))
            val remote = RemoteServices(hevyUrl = server.url("/").toString().trimEnd('/'))
            val workouts = remote.workouts("test-key", LocalDate.of(2026,10,19), ZoneId.of("Europe/Istanbul"))
            assertEquals(1, workouts.size); assertEquals("hevy:new", workouts.first().id)
            assertTrue(workouts.first().rawJson!!.contains("distance_meters")); assertTrue(workouts.first().notes.contains("2000 m"))
            assertEquals("test-key", server.takeRequest().getHeader("api-key"))
            assertTrue(server.takeRequest().path!!.contains("page=2"))
        }
    }
    @Test fun `DeepSeek sends exact snapshot and rejects incomplete output`() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse().setBody("""{"choices":[{"finish_reason":"stop","message":{"content":"Gün özeti"}}]}"""))
            val remote = RemoteServices(deepseekUrl = server.url("/chat/completions").toString())
            val exact = "{\"history\":\"tam kayıt\"}"
            assertEquals("Gün özeti", remote.recap("test-key", "deepseek-flash", exact))
            val request = server.takeRequest()
            assertEquals("Bearer test-key", request.getHeader("Authorization"))
            val body = JsonCodec.parseToJsonElement(request.body.readUtf8()).jsonObject
            assertEquals("disabled", body.getValue("thinking").jsonObject.getValue("type").jsonPrimitive.content)
            assertEquals(exact, body.getValue("messages").jsonArray[1].jsonObject.getValue("content").jsonPrimitive.content)
            server.enqueue(MockResponse().setBody("""{"choices":[{"finish_reason":"length","message":{"content":"yarım"}}]}"""))
            try { remote.recap("key", "model", exact); fail("Partial output must fail") } catch (_: IllegalArgumentException) { }
        }
    }
    @Test fun `API errors do not leak response bodies or keys`() = runTest {
        MockWebServer().use { server ->
            server.start(); server.enqueue(MockResponse().setResponseCode(401).setBody("secret-server-body"))
            try { RemoteServices(deepseekUrl = server.url("/").toString()).recap("secret-key", "model", "private-payload"); fail() }
            catch (e: ApiException) { assertEquals(401, e.status); assertFalse(e.message!!.contains("secret")); assertFalse(e.message!!.contains("private")) }
        }
    }
    @Test fun `Hevy verifies account access without revealing a key`() = runTest {
        MockWebServer().use { server ->
            server.start(); server.enqueue(MockResponse().setBody("""{"workout_count":42}"""))
            val api = RemoteServices(hevyUrl = server.url("/").toString().trimEnd('/'))
            assertEquals(42, api.verifyHevy("test-key"))
            val request = server.takeRequest()
            assertEquals("/v1/workouts/count", request.path)
            assertEquals("test-key", request.getHeader("api-key"))
        }
    }
    @Test fun `Hevy history changed during pagination is rejected rather than partially imported`() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse().setBody("""{"page_count":2,"workouts":[]}"""))
            server.enqueue(MockResponse().setBody("""{"page_count":3,"workouts":[]}"""))
            val api = RemoteServices(hevyUrl = server.url("/").toString().trimEnd('/'))
            try { api.fetchHevyHistory("key", ZoneId.of("Europe/Istanbul")); fail("Changed source must fail") }
            catch (_: java.io.IOException) { }
        }
    }
    @Test fun `empty Hevy accounts are supported and every page reports progress`() = runTest {
        MockWebServer().use { server ->
            server.start(); server.enqueue(MockResponse().setBody("""{"page_count":0,"workouts":[]}"""))
            val progress = mutableListOf<Pair<Int, Int>>()
            val result = RemoteServices(hevyUrl = server.url("/").toString().trimEnd('/')).fetchHevyHistory("key", ZoneId.of("Europe/Istanbul")) { page, count -> progress += page to count }
            assertTrue(result.isEmpty()); assertEquals(listOf(1 to 1), progress)
        }
    }
    @Test fun `timed strength sets do not become fabricated cardio sessions`() = runTest {
        MockWebServer().use { server ->
            server.start(); server.enqueue(MockResponse().setBody("""{"page_count":1,"workouts":[{"id":"plank","title":"Core","start_time":"2026-10-19T15:00:00Z","end_time":"2026-10-19T15:30:00Z","exercises":[{"title":"Plank","sets":[{"duration_seconds":60}]}]}]}"""))
            val result = RemoteServices(hevyUrl = server.url("/").toString().trimEnd('/')).fetchHevyHistory("key", ZoneId.of("Europe/Istanbul"))
            assertEquals("Hevy antrenmanı", result.single().workout.kind)
            assertTrue(result.single().workout.notes.contains("60 sn"))
        }
    }

}
