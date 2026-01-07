package com.jayala.vexapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.jayala.vexapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences("VexPrefs", Context.MODE_PRIVATE)
        val savedTeam = sharedPref.getString("team_number", null)

        // 1. If stored, go to Home immediately
        if (savedTeam != null) {
            navigateToHome()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.searchButton.setOnClickListener {
            val teamInput = binding.teamNumberSearch.text.toString().trim().uppercase()
            if (teamInput.isNotEmpty()) {
                // 2. Store the number
                sharedPref.edit { putString("team_number", teamInput) }
                // 3. Go to Home
                navigateToHome()
            }
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish() // This prevents the user from hitting "back" to the search screen
    }
}