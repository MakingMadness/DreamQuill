/*
Todo: Store the API key in Android, changeable through settings.
Todo: Add a copy to clipboard button.
Todo: Add a dropdown box of ChatGPT models.
Todo: Add inputs to change repetition.
Todo: Allow the default prompt to be changed.
Todo: Add a scrollbar for the text area.
Todo: Give feedback when awaiting a response.
Todo: Increase the timeout.
 */

package com.makingmadness.dreamquill

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.cjcrafter.openai.OpenAI
import com.cjcrafter.openai.chat.ChatMessage.Companion.toSystemMessage
import com.cjcrafter.openai.chat.ChatMessage.Companion.toUserMessage
import com.cjcrafter.openai.chat.ChatRequest
import com.cjcrafter.openai.exception.WrappedIOError
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var inputEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var undoButton: Button
    private lateinit var clearButton: Button

    private val prompt = "Please respond in markdown."
    private val messages = mutableListOf(prompt.toSystemMessage())
    private val request = ChatRequest(model = "gpt-3.5-turbo", messages = messages)

    private val key = BuildConfig.OPENAI_KEY
    private val openai = OpenAI(key)

    private val undoRedoManager = UndoRedoManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        inputEditText = findViewById(R.id.inputEditText)
        sendButton = findViewById(R.id.sendButton)
        undoButton = findViewById(R.id.undoButton)
        clearButton = findViewById(R.id.clearButton)

        undoButton.isEnabled = false
    }

    private fun setupListeners() {
        sendButton.setOnClickListener {
            handleSendButtonClick()
        }

        undoButton.setOnClickListener {
            undoRedoManager.toggleUndoRedo()
        }

        clearButton.setOnClickListener {
            inputEditText.setText("")
        }

        inputEditText.addTextChangedListener(createTextWatcher())
    }

    private fun createTextWatcher() = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable) {
            updateUndoButtonState()
        }
    }

    private fun updateUndoButtonState() {
        if (undoRedoManager.isUserTyping) {
            undoButton.isEnabled = false
            undoButton.text = "Undo"
        }
    }

    private fun handleSendButtonClick() {
        val input = inputEditText.text.toString()
        if (input.isNotBlank()) {
            messages.add(input.toUserMessage())
            requestResponseAndUpdateUI()
        }
    }

    private fun requestResponseAndUpdateUI() {
        CoroutineScope(Dispatchers.Main).launch {
            val response = getOpenAIResponseAsync()
            undoRedoManager.addUndoableOperation(response)
            if (response.isNotBlank()) {
                inputEditText.append("\n\n$response\n\n")
                undoButton.isEnabled = true
            }
        }
    }

    private suspend fun getOpenAIResponseAsync(): String = withContext(Dispatchers.IO) {
        val layout = findViewById<ConstraintLayout>(R.id.constraint_layout)
        return@withContext try {
            val response = openai.createChatCompletion(request)
            val message = response.get(0)?.message?.content
            response.get(0)?.message?.let { messages.add(it) }
            message ?: ""
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


    private inner class UndoRedoManager {
        private var undoStack = mutableListOf<String>()
        private var redoStack = mutableListOf<String>()
        private var inUndoRedoMode = false

        val isUserTyping: Boolean
            get() = !inUndoRedoMode

        fun addUndoableOperation(text: String) {
            if (!inUndoRedoMode) {
                undoStack.add(text)
                redoStack.clear()
            }
        }

        fun toggleUndoRedo() {
            inUndoRedoMode = true
            if (redoStack.isEmpty()) {
                performUndo()
            } else {
                performRedo()
            }
            inUndoRedoMode = false
        }

        private fun performUndo() {
            if (undoStack.isNotEmpty()) {
                val lastText = undoStack.removeLast()
                updateInputEditText { currentText ->
                    currentText.removeSuffix("\n\n$lastText\n\n")
                }
                redoStack.add(lastText)
                undoButton.text = "Redo"
            }
        }

        private fun performRedo() {
            if (redoStack.isNotEmpty()) {
                val lastText = redoStack.removeLast()
                updateInputEditText { currentText ->
                    "$currentText\n\n$lastText\n\n"
                }
                undoStack.add(lastText)
                undoButton.text = "Undo"
            }
        }

        private fun updateInputEditText(update: (String) -> String) {
            inputEditText.apply {
                val currentText = text.toString()
                val newText = update(currentText)
                setText(newText)
                setSelection(newText.length)
            }
        }
    }
}
