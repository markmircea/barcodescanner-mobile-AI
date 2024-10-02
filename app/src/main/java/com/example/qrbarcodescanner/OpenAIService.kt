package com.example.qrbarcodescanner

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface OpenAIApi {
    @Headers("Authorization: Bearer ${ApiKeys.OPENAI_API_KEY}")
    @POST("v1/chat/completions")
    suspend fun getChatCompletion(@Body body: ChatCompletionRequest): ChatCompletionResponse
}

data class ChatCompletionRequest(
    val model: String = "gpt-4o-mini",
    val messages: List<Message>
)

data class Message(
    val role: String,
    val content: String
)

data class ChatCompletionResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)

object OpenAIService {
    private const val TAG = "OpenAIService"
    private const val RATE_LIMIT_DELAY = 60_000L // 1 minute in milliseconds

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openai.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api: OpenAIApi = retrofit.create(OpenAIApi::class.java)
    private val mutex = Mutex()
    private var lastApiCallTime = 0L

    suspend fun getDescriptionForBarcode(barcodeContent: String): String {
        return try {
            mutex.withLock {
                val currentTime = System.currentTimeMillis()
                val timeSinceLastCall = currentTime - lastApiCallTime
                if (timeSinceLastCall < RATE_LIMIT_DELAY) {
                    val delayTime = RATE_LIMIT_DELAY - timeSinceLastCall
                    Log.d(TAG, "Rate limit reached. Waiting for $delayTime ms")
                    kotlinx.coroutines.delay(delayTime)
                }

                Log.d(TAG, "Fetching description for barcode: $barcodeContent")
                val messages = listOf(
                    Message(
                        role = "system",
                        content = "You are a helpful assistant that provides brief descriptions for barcode or QR code content."
                    ),
                    Message(
                        role = "user",
                        content = "Provide a brief description for the following barcode or QR code content: $barcodeContent"
                    )
                )
                val request = ChatCompletionRequest(messages = messages)
                Log.d(TAG, "Sending request to OpenAI API: $request")
                val response = api.getChatCompletion(request)
                Log.d(TAG, "Received response from OpenAI API: $response")
                val description = response.choices.firstOrNull()?.message?.content?.trim() ?: "No description available"
                Log.d(TAG, "Description fetched: $description")

                lastApiCallTime = System.currentTimeMillis()
                description
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching description", e)
            "Error fetching description: ${e.message}"
        }
    }
}