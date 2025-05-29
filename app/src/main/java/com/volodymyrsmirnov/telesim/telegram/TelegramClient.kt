package com.volodymyrsmirnov.telesim.telegram

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Duration

class TelegramClient {
    class RetryException() : Exception()

    class FatalException() : Exception()

    @Serializable
    private data class TelegramSendMessageRequest(
        @SerialName("chat_id") val chatId: String,
        @SerialName("text") val text: String,
        @SerialName("parse_mode") val parseMode: String
    )

    private val clientTimeout = Duration.ofSeconds(10)

    private val client = OkHttpClient.Builder()
        .connectTimeout(clientTimeout)
        .readTimeout(clientTimeout)
        .writeTimeout(clientTimeout)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    internal suspend fun sendMessage(token: String, chatId: String, message: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = TelegramSendMessageRequest(chatId, message, "HTML")

                val jsonBody = json.encodeToString(TelegramSendMessageRequest.serializer(), requestBody)

                val request = Request.Builder()
                    .url("https://api.telegram.org/bot$token/sendMessage")
                    .post(jsonBody.toRequestBody("application/json".toMediaType()))
                    .build()

                Log.i("TelegramService", "Sending request: $jsonBody")

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: "No response body"

                if (response.isSuccessful) {
                    Log.i("TelegramService", "Message sent successfully: $responseBody")
                    Result.success(Unit)
                } else {
                    Log.e("TelegramService", "Message sent failed (${response.code}): $responseBody")

                    if (response.code == 429 || response.code in 500..599) {
                        throw RetryException()
                    } else {
                        throw FatalException()
                    }
                }
            } catch (e: Exception) {
                Log.e("TelegramService", "Error sending message", e)
                Result.failure(e)
            }
        }
    }
}