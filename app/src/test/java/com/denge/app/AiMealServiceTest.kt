package com.denge.app

import com.denge.app.domain.*
import com.denge.app.integration.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class AiMealServiceTest {
    private val food = """{"name":"Tavuk ve pilav","category":"Öğle","time":"13:00","calories":650,"protein":45,"carbs":null,"fat":18,"note":"150 g tavuk varsayıldı"}"""
    private fun response(content: String, finish: String = "stop") = buildJsonObject { put("choices", buildJsonArray { add(buildJsonObject {
        put("finish_reason", finish); put("message", buildJsonObject { put("content", content) })
    }) }) }.toString()
    @Test fun `meal request uses JSON output and only sends text and defaults`() = runTest {
        MockWebServer().use { server ->
            server.start(); server.enqueue(MockResponse().setBody(response("""{"meals":[$food],"message":"Kontrol et"}""")))
            val result = RemoteServices(deepseekUrl = server.url("/").toString()).understandMeals("test-key", "deepseek-flash", "Tavuk pilav yedim, karbonhidrat bilinmiyor", "Öğle", "13:00")
            assertEquals(650, result.first.single().calories); assertNull(result.first.single().carbs)
            assertEquals("150 g tavuk varsayıldı", result.first.single().aiNote)
            val request = server.takeRequest()
            assertEquals("Bearer test-key", request.getHeader("Authorization"))
            val json = JsonCodec.parseToJsonElement(request.body.readUtf8()).jsonObject
            assertEquals("json_object", json.getValue("response_format").jsonObject.getValue("type").jsonPrimitive.content)
            val user = JsonCodec.parseToJsonElement(json.getValue("messages").jsonArray[1].jsonObject.getValue("content").jsonPrimitive.content).jsonObject
            assertEquals(setOf("text", "defaultCategory", "defaultTime"), user.keys)
            assertEquals("Tavuk pilav yedim, karbonhidrat bilinmiyor", user.getValue("text").jsonPrimitive.content)
        }
    }
    @Test fun `partial malformed and out of range results never return partial meals`() = runTest {
        MockWebServer().use { server ->
            server.start(); val remote = RemoteServices(deepseekUrl = server.url("/").toString())
            listOf(response("""{"meals":[$food],"message":""}""", "length"), response("not-json"), response("""{"meals":[$food,${food.replace("650", "-1")}],"message":""}"""), response("")).forEach { body ->
                server.enqueue(MockResponse().setBody(body))
                assertTrue(runCatching { remote.understandMeals("key", "model", "private-input", "Öğle", "13:00") }.isFailure)
            }
        }
    }
    @Test fun `clarification is shown without creating a record`() = runTest {
        MockWebServer().use { server ->
            server.start(); server.enqueue(MockResponse().setBody(response("""{"meals":[],"message":"Hangi yemeği ve ne kadar yedin?"}""")))
            val result = RemoteServices(deepseekUrl = server.url("/").toString()).understandMeals("key", "model", "yemek yedim", "Öğle", "13:00")
            assertTrue(result.first.isEmpty()); assertTrue(result.second.contains("ne kadar"))
        }
    }
    @Test fun `meal API failure hides service body and personal input`() = runTest {
        MockWebServer().use { server ->
            server.start(); server.enqueue(MockResponse().setResponseCode(401).setBody("secret-key private-input"))
            val error = runCatching { RemoteServices(deepseekUrl = server.url("/").toString()).understandMeals("secret-key", "model", "private-input", "Öğle", "13:00") }.exceptionOrNull()!!
            assertTrue(error is ApiException); assertFalse(error.message!!.contains("secret-key")); assertFalse(error.message!!.contains("private-input"))
        }
    }
}
