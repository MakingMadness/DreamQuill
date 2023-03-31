package com.makingmadness.dreamquill

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SettingsActivity : AppCompatActivity() {

    private lateinit var apiKeyEditText: EditText
    private lateinit var timeoutEditText: EditText
    private lateinit var saveButton: Button

    private lateinit var encryptedSharedPreferences: SharedPreferences
    private lateinit var sharedPreferences: SharedPreferences

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

        sharedPreferences = getSharedPreferences("DREAMQUILL_PREFS", MODE_PRIVATE)

        apiKeyEditText = findViewById(R.id.api_key_edit_text)
        timeoutEditText = findViewById(R.id.timeout_edit_text)
        saveButton = findViewById(R.id.save_button)

        loadApiKey()
        loadTimeout()

        saveButton.setOnClickListener {
            saveApiKey()
            saveTimeout()
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
        val timeout = sharedPreferences.getInt("TIMEOUT", 30)
        timeoutEditText.setText(timeout.toString())
    }

    private fun saveTimeout() {
        val timeout = timeoutEditText.text.toString().toIntOrNull() ?: 0
        sharedPreferences.edit().putInt("TIMEOUT", timeout).apply()
    }
}
