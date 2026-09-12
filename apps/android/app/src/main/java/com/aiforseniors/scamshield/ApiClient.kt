package com.aiforseniors.scamshield

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable
data class ClassifyRequest(val source: String, val text: String)

@Serializable
data class ClassifyResult(
    val isScam: Boolean = false,
    val reason: String = "",
    val seniorWarning: String = "",
    val caregiverMessage: String = "",
    val mode: String = "",
)

@Serializable
data class Vip(val id: String = "", val name: String = "", val phone: String = "")

@Serializable
data class VipsResponse(val vips: List<Vip> = emptyList())

class ApiClient(
    private val baseUrl: String,
    private val deviceToken: String,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    fun classifyScam(text: String, source: String = "whatsapp"): ClassifyResult {
        val body = json.encodeToString(
            ClassifyRequest.serializer(),
            ClassifyRequest(source = source, text = text),
        ).toRequestBody("application/json".toMediaType())
        val req = Request.Builder()
            .url("$baseUrl/api/classify-scam")
            .header("X-Device-Token", deviceToken)
            .post(body)
            .build()
        http.newCall(req).execute().use { res ->
            val raw = res.body?.string().orEmpty()
            if (!res.isSuccessful) {
                throw IllegalStateException("classify HTTP ${res.code}: $raw")
            }
            return json.decodeFromString(ClassifyResult.serializer(), raw)
        }
    }

    fun listVips(): List<Vip> {
        val req = Request.Builder()
            .url("$baseUrl/api/vips")
            .header("X-Device-Token", deviceToken)
            .get()
            .build()
        http.newCall(req).execute().use { res ->
            val raw = res.body?.string().orEmpty()
            if (!res.isSuccessful) {
                throw IllegalStateException("vips HTTP ${res.code}: $raw")
            }
            return json.decodeFromString(VipsResponse.serializer(), raw).vips
        }
    }
}
