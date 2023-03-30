package com.makingmadness.dreamquill

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SettingsActivity : AppCompatActivity() {

    private lateinit var apiKeyEditText: EditText
    private lateinit var saveApiKeyButton: Button

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        sharedPreferences = EncryptedSharedPreferences.create(
            this,
            "API_KEY_PREFS",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        apiKeyEditText = findViewById(R.id.api_key_edit_text)
        saveApiKeyButton = findViewById(R.id.save_api_key_button)

        loadApiKey()

        saveApiKeyButton.setOnClickListener {
            saveApiKey()
        }
    }

    private fun loadApiKey() {
        val apiKey = sharedPreferences.getString("API_KEY", "")
        apiKeyEditText.setText(apiKey)
    }

    private fun saveApiKey() {
        val apiKey = apiKeyEditText.text.toString()
        sharedPreferences.edit().putString("API_KEY", apiKey).apply()
    }
}