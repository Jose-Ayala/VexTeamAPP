package com.jayala.vexapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.jayala.vexapp.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Check for saved data
        val sharedPref = getSharedPreferences("VexPrefs", MODE_PRIVATE)
        val savedTeam = sharedPref.getString("team_number", null)

        if (savedTeam != null) {
            navigateToHome()
            return
        }

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.searchButton.setOnClickListener {
            val teamInput = binding.teamNumberSearch.text.toString().trim().uppercase()

            if (validateFormat(teamInput)) {
                verifyTeamExists(teamInput, sharedPref)
            }
        }
    }

    private fun validateFormat(input: String): Boolean {
        val vexRegex = Regex("^[0-9A-Z]{2,10}$")

        return when {
            input.isEmpty() -> {
                binding.teamNumberInputLayout.error = "Please enter a team number"
                false
            }
            !input.matches(vexRegex) -> {
                binding.teamNumberInputLayout.error = "Use format like 1234A"
                false
            }
            else -> {
                binding.teamNumberInputLayout.error = null
                true
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun verifyTeamExists(teamNumber: String, sharedPref: android.content.SharedPreferences) {
        lifecycleScope.launch {
            try {
                binding.searchButton.isEnabled = false
                binding.searchButton.text = "VERIFYING..."

                val response = RetrofitClient.service.getTeamInfo(teamNumber = teamNumber)

                if (response.isSuccessful && response.body()?.data?.isNotEmpty() == true) {
                    sharedPref.edit { putString("team_number", teamNumber) }
                    navigateToHome()
                } else {
                    binding.teamNumberInputLayout.error = "Team not found, please enter a different team number."
                }
            } catch (_: Exception) {
                // ERROR: Network/Connection issue
                binding.teamNumberInputLayout.error = "Connection error. Try again."
            } finally {
                binding.searchButton.isEnabled = true
                binding.searchButton.text = "SEARCH"
            }
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}