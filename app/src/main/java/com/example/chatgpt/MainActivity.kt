package com.example.chatgpt

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cjcrafter.openai.OpenAI
import com.cjcrafter.openai.chat.ChatMessage.Companion.toSystemMessage
import com.cjcrafter.openai.chat.ChatMessage.Companion.toUserMessage
import com.cjcrafter.openai.chat.ChatRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {

    private lateinit var inputEditText: EditText
    private lateinit var sendButton: Button

    private val prompt = "Be as unhelpful as possible"
    private val messages = mutableListOf(prompt.toSystemMessage())
    private val request = ChatRequest(model = "gpt-3.5-turbo", messages = messages)

    private val key = BuildConfig.OPENAI_KEY
    private val openai = OpenAI(key)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        inputEditText = findViewById(R.id.inputEditText)
        sendButton = findViewById(R.id.sendButton)
    }

    private fun setupListeners() {
        sendButton.setOnClickListener {
            handleSendButtonClick()
        }
    }

    private fun handleSendButtonClick() {
        val input = inputEditText.text.toString()
        if (input.isNotBlank()) {
            messages.add(input.toUserMessage())
            CoroutineScope(Dispatchers.Main).launch {
                val response = getOpenAIResponseAsync()
                inputEditText.append("\n\n$response\n")
            }
        }
    }

    private suspend fun getOpenAIResponseAsync(): String = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = openai.createChatCompletion(request)
            val message = response.get(0)?.message?.content
            response.get(0)?.message?.let { messages.add(it) }
            message ?: ""
        } catch (e: Exception) {
            Log.e("GetOpenAIResponse", "Error in getOpenAIResponseAsync", e)
            "Error occurred while fetching response."
        }
    }
}
