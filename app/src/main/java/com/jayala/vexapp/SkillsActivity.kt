package com.jayala.vexapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jayala.vexapp.databinding.ActivitySkillsBinding
import kotlinx.coroutines.launch

class SkillsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySkillsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySkillsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get the ID from HomeActivity
        val teamId = intent.getIntExtra("TEAM_ID", -1)

        if (teamId != -1) {
            fetchSkillsData(teamId)
        } else {
            Toast.makeText(this, "Error: No team ID found", Toast.LENGTH_SHORT).show()
        }

        // Back Button Logic
        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun fetchSkillsData(teamId: Int) {
        lifecycleScope.launch {
            try {
                Log.d("VEX_DEBUG", "Fetching skills for Team ID: $teamId")

                // Requesting seasons
                val allSeasonIds = (189..202).toList()
                val response = RetrofitClient.service.getTeamSkills(
                    teamId = teamId,
                    seasons = allSeasonIds
                )

                if (response.isSuccessful && response.body() != null) {
                    val rawData = response.body()!!.data

                    val groupedByEvent = rawData.groupBy { dataItem: SkillsData ->
                        dataItem.event.name to dataItem.season.name
                    }

                    val uiModels = groupedByEvent.map { (key, entries) ->
                        val (eventName, seasonName) = key

                        // Pair Driver and Programming scores for the same event
                        val driver = entries.find { it.type == "driver" }?.score ?: 0
                        val programming = entries.find { it.type == "programming" }?.score ?: 0

                        SkillsUiModel(
                            eventName = eventName,
                            seasonName = seasonName,
                            driverScore = driver,
                            programmingScore = programming,
                            totalScore = driver + programming
                        )
                    }.reversed()

                    binding.skillsRecyclerView.adapter = SkillsAdapter(uiModels)

                    Log.d("VEX_DEBUG", "Successfully displayed ${uiModels.size} merged events")

                } else {
                    Log.e("VEX_DEBUG", "API Error: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("VEX_DEBUG", "Skills Fetch Failed", e)
                Toast.makeText(applicationContext, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

data class SkillsUiModel(
    val eventName: String,
    val seasonName: String,
    val driverScore: Int,
    val programmingScore: Int,
    val totalScore: Int
)