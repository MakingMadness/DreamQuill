package com.makingmadness.dreamquill

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.cjcrafter.openai.OpenAI
import com.cjcrafter.openai.chat.ChatRequest
import com.cjcrafter.openai.exception.WrappedIOError
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AIChatManager(private val openai: OpenAI, private val request: ChatRequest) {
    suspend fun getOpenAIResponseAsync(activity: AppCompatActivity): String = withContext(
        Dispatchers.IO) {
        val layout = activity.findViewById<ConstraintLayout>(R.id.constraint_layout)
        return@withContext try {
            val response = openai.createChatCompletion(request)
            val message = response[0].message.content
            response[0].message.let { request.messages.add(it) }
            message
        } catch (e: WrappedIOError) {
            Log.e("GetOpenAIResponse", "Error in getOpenAIResponseAsync", e)
            withContext(Dispatchers.Main) {
                if (e.message?.contains("timeout") == true) {
                    Snackbar.make(layout, "Request timed out.", Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(layout, "An IO error occurred while fetching a response.", Snackbar.LENGTH_SHORT).show()
                }
            }
            ""
        } catch (e: Exception) {
            Log.e("GetOpenAIResponse", "Error in getOpenAIResponseAsync", e)
            withContext(Dispatchers.Main) {
                Snackbar.make(layout, "An unknown error occurred while fetching a response.", Snackbar.LENGTH_SHORT).show()
            }
            ""
        }
    }
}