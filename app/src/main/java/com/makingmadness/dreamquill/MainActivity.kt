/*
Todo: Add a dropdown box of ChatGPT models.
Todo: Add inputs to change repetition.
Todo: Allow the default prompt to be changed.
Todo: Increase the timeout.
*/

package com.makingmadness.dreamquill

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.cjcrafter.openai.OpenAI
import com.cjcrafter.openai.chat.ChatMessage.Companion.toSystemMessage
import com.cjcrafter.openai.chat.ChatMessage.Companion.toUserMessage
import com.cjcrafter.openai.chat.ChatRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var inputEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var undoButton: Button
    private lateinit var clearButton: Button
    private lateinit var copyButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var settingsButton: Button
    private lateinit var openai: OpenAI
    private lateinit var aiChatManager: AIChatManager

    private val prompt = "Please respond in markdown."
    private val messages = mutableListOf(prompt.toSystemMessage())
    private val request = ChatRequest(model = "gpt-3.5-turbo", messages = messages)

    private val undoRedoManager = UndoRedoManager()

    private val coroutineJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + coroutineJob)

    private val key by lazy {
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val sharedPreferences = EncryptedSharedPreferences.create(
            this,
            "API_KEY_PREFS",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        sharedPreferences.getString("API_KEY", "") ?: ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        openai = OpenAI(key)

        initAIChatManager()

        progressBar = findViewById(R.id.progressBar)
        initViews()
        setupListeners()
    }

    override fun onDestroy() {
        coroutineJob.cancel()
        super.onDestroy()
    }

    private fun initAIChatManager() {
        aiChatManager = AIChatManager(openai, request)
    }

    private fun initViews() {
        inputEditText = findViewById(R.id.inputEditText)
        sendButton = findViewById(R.id.sendButton)
        undoButton = findViewById(R.id.undoButton)
        clearButton = findViewById(R.id.clearButton)
        settingsButton = findViewById(R.id.settingsButton)
        copyButton = findViewById(R.id.copyButton)

        undoButton.isEnabled = false
        progressBar.visibility = View.GONE
    }

    private fun setupListeners() {
        sendButton.setOnClickListener {
            handleSendButtonClick()
        }

        undoButton.setOnClickListener {
            undoRedoManager.toggleUndoRedo({ newText ->
                inputEditText.apply {
                    setText(newText)
                    setSelection(newText.length)
                }
            }, ::updateUndoButtonText, inputEditText.text.toString())
        }

        clearButton.setOnClickListener {
            inputEditText.setText("")
        }

        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        copyButton.setOnClickListener {
            copyTextToClipboard()
        }

        inputEditText.addTextChangedListener(createTextWatcher())
    }

    private fun copyTextToClipboard() {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("DreamQuillAppText", inputEditText.text.toString())
        clipboardManager.setPrimaryClip(clipData)
        showToast("Text copied to clipboard")
    }

    private fun updateUndoButtonText(newText: String) {
        undoButton.text = newText
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
        coroutineScope.launch {

            // Disable UI while fetching.
            progressBar.visibility = View.VISIBLE
            inputEditText.isEnabled = false
            clearButton.isEnabled = false
            sendButton.isEnabled = false
            undoButton.isEnabled = false

            // Fetch a response.
            val response = aiChatManager.getOpenAIResponseAsync(this@MainActivity)

            // Re-enable UI.
            progressBar.visibility = View.GONE
            inputEditText.isEnabled = true
            clearButton.isEnabled = true
            sendButton.isEnabled = true

            undoRedoManager.addUndoableOperation(response)
            if (response.isNotBlank()) {
                inputEditText.append("\n\n$response\n\n")
                undoButton.isEnabled = true
            }
        }
    }
}