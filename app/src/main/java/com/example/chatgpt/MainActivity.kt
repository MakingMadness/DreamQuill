package com.example.chatgpt

import android.os.AsyncTask
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
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {

    private lateinit var inputEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var chatTextView: TextView

    private val prompt = "Be as unhelpful as possible"
    private val messages = mutableListOf(prompt.toSystemMessage())
    private val request = ChatRequest(model="gpt-3.5-turbo", messages=messages)

    private val key = BuildConfig.OPENAI_KEY
    private val openai = OpenAI(key)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inputEditText = findViewById(R.id.inputEditText)
        sendButton = findViewById(R.id.sendButton)
        chatTextView = findViewById(R.id.chatTextView)

        sendButton.setOnClickListener {
            val input = inputEditText.text.toString()
            if (input.isNotBlank()) {
                messages.add(input.toUserMessage())
                inputEditText.setText("")
                GetOpenAIResponse(this).execute()
            }
        }
    }

    private class GetOpenAIResponse(context: MainActivity) : AsyncTask<Void, Void, String>() {
        private val activityReference: WeakReference<MainActivity> = WeakReference(context)

        override fun doInBackground(vararg params: Void?): String {
            return try {
                val activity = activityReference.get()
                val response = activity?.openai?.createChatCompletion(activity.request)
                val message = response?.get(0)?.message?.content
                response?.get(0)?.message?.let { activity?.messages?.add(it) }
                message ?: ""
            } catch (e: Exception) {
                Log.e("GetOpenAIResponse", "Error in doInBackground", e)
                "Error occurred while fetching response."
            }
        }

        override fun onPostExecute(result: String) {
            activityReference.get()?.apply {
                chatTextView.append("\nUser: ${messages.last().content}\n")
                chatTextView.append("Bot: $result\n")
            }
        }
    }
}
