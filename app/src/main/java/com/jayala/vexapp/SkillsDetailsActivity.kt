package com.jayala.vexapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jayala.vexapp.databinding.ActivitySkillsDetailsBinding
import kotlinx.coroutines.launch

class SkillsDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySkillsDetailsBinding
    private var eventId: Int = -1
    private var currentTeamId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySkillsDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        eventId = intent.getIntExtra("EVENT_ID", -1)
        val sharedPref = getSharedPreferences("VexPrefs", MODE_PRIVATE)
        currentTeamId = sharedPref.getInt("team_id", -1)

        binding.backButton.setOnClickListener { finish() }
        binding.skillsRecyclerView.layoutManager = LinearLayoutManager(this)

        if (eventId != -1) {
            fetchFullLeaderboard()
        }
    }

    private fun fetchFullLeaderboard() {
        binding.progressBar.visibility = android.view.View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.service.getEventSkills(eventId)
                if (response.isSuccessful && response.body() != null) {
                    val rawData = response.body()!!.data
                    processAndDisplaySkills(rawData)
                }
            } catch (e: Exception) {
                Log.e("SKILLS_DEBUG", "Failed to fetch leaderboard", e)
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }

    private fun processAndDisplaySkills(data: List<CompSkillData>) {
        @Suppress("UNCHECKED_CAST")
        val teamNameMap = intent.getSerializableExtra("TEAM_MAP") as? HashMap<Int, String> ?: hashMapOf()

        val summaries = data.groupBy { it.team?.id ?: -1 }
            .filter { it.key != -1 }
            .map { entry ->
                val teamId = entry.key
                val firstEntry = entry.value.first()

                val teamNumber = firstEntry.team?.name ?: "????"

                val teamName = teamNameMap[teamId] ?: ""

                val driver = entry.value.filter { it.type == "driver" }.maxOfOrNull { it.score } ?: 0
                val prog = entry.value.filter { it.type == "programming" }.maxOfOrNull { it.score } ?: 0
                val rank = entry.value.map { it.rank }.firstOrNull { it > 0 } ?: 0

                TeamSkillSummary(teamId, teamNumber, driver, prog, driver + prog, rank, teamName)
            }
            .sortedBy { it.rank }

        binding.skillsRecyclerView.adapter = SkillsLeaderboardAdapter(summaries, currentTeamId, teamNameMap)
    }
}