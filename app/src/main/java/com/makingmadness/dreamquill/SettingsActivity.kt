package com.makingmadness.dreamquill

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.content.Intent
import android.net.Uri
import android.widget.TextView

class SettingsActivity : AppCompatActivity() {

    private lateinit var apiKeyEditText: EditText
    private lateinit var timeoutEditText: EditText
    private lateinit var promptPrefixEditText: EditText

    private lateinit var encryptedSharedPreferences: SharedPreferences
    private lateinit var timeoutSharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        encryptedSharedPreferences = EncryptedSharedPreferences.create(
            this,
            "API_KEY_PREFS",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        timeoutSharedPreferences = getSharedPreferences("DREAMQUILL_PREFS", MODE_PRIVATE)

        apiKeyEditText = findViewById(R.id.api_key_edit_text)
        timeoutEditText = findViewById(R.id.timeout_edit_text)
        promptPrefixEditText = findViewById(R.id.prompt_prefix_edit_text)

        loadApiKey()
        loadTimeout()
        loadPromptPrefix()

        apiKeyEditText.addTextChangedListener { saveApiKey() }
        timeoutEditText.addTextChangedListener { saveTimeout() }
        promptPrefixEditText.addTextChangedListener { savePromptPrefix() }

        val openaiLink: TextView = findViewById(R.id.openai_link)
        openaiLink.setOnClickListener {
            val openUrlIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://platform.openai.com/signup"))
            openUrlIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(openUrlIntent)
        }
    }

    private fun loadApiKey() {
        val apiKey = encryptedSharedPreferences.getString("API_KEY", "")
        apiKeyEditText.setText(apiKey)
    }

    private fun saveApiKey() {
        val apiKey = apiKeyEditText.text.toString()
        encryptedSharedPreferences.edit().putString("API_KEY", apiKey).apply()
    }

    private fun loadTimeout() {
        val timeout = timeoutSharedPreferences.getInt("TIMEOUT", 0)
        timeoutEditText.setText(timeout.toString())
    }

    private fun saveTimeout() {
        val timeout = timeoutEditText.text.toString().toIntOrNull() ?: 0
        timeoutSharedPreferences.edit().putInt("TIMEOUT", timeout).apply()
    }

    private fun loadPromptPrefix() {
        val promptPrefix = timeoutSharedPreferences.getString("PROMPT_PREFIX", "Please respond in markdown format.")
        promptPrefixEditText.setText(promptPrefix)
    }

    private fun savePromptPrefix() {
        val promptPrefix = promptPrefixEditText.text.toString()
        timeoutSharedPreferences.edit().putString("PROMPT_PREFIX", promptPrefix).apply()
    }

    private fun EditText.addTextChangedListener(afterTextChanged: (Editable) -> Unit) {
        this.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                afterTextChanged(s)
            }
        })
    }
}
