package com.jayala.vexapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.jayala.vexapp.databinding.ActivityHomeBinding
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private var teamId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPref = getSharedPreferences("VexPrefs", MODE_PRIVATE)
        val savedTeam = sharedPref.getString("team_number", null)

        if (savedTeam != null) {
            fetchTeamData(savedTeam)
        }

        binding.skillsButton.setOnClickListener {
            val id = teamId
            if (id != null) {
                val intent = Intent(this, SkillsActivity::class.java)
                intent.putExtra("TEAM_ID", id)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Team data still loading...", Toast.LENGTH_SHORT).show()
            }
        }

        binding.competitionsButton.setOnClickListener {
            val intent = Intent(this, CompsActivity::class.java)
            intent.putExtra("TEAM_ID", teamId)
            startActivity(intent)
        }

        binding.awardsButton.setOnClickListener {
            val id = teamId
            if (id != null) {
                val intent = Intent(this, AwardsActivity::class.java)
                intent.putExtra("TEAM_ID", id)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Team data still loading...", Toast.LENGTH_SHORT).show()
            }
        }

        binding.changeTeamButton.setOnClickListener {
            sharedPref.edit { remove("team_number") }
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun fetchTeamData(teamNumber: String) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.service.getTeamInfo(teamNumber = teamNumber)

                if (response.isSuccessful && response.body()?.data?.isNotEmpty() == true) {
                    val team = response.body()!!.data[0]
                    teamId = team.id

                    binding.teamName.text = getString(R.string.team_name_format, team.number, team.team_name)
                    binding.programName.text = getString(R.string.program_format, team.program.code, team.program.name)
                    binding.organization.text = team.organization
                    binding.location.text = getString(R.string.location_format, team.location.city, team.location.region)
                    binding.country.text = team.location.country

                } else {
                    Log.e("VEX_DEBUG", "API Error: ${response.code()}")
                    Toast.makeText(this@HomeActivity, "Team not found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("VEX_DEBUG", "Connection failure", e)
                Toast.makeText(this@HomeActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}