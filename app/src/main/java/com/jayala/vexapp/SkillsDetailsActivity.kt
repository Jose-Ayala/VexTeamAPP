package com.jayala.vexapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jayala.vexapp.databinding.ActivitySkillsDetailsBinding
import kotlinx.coroutines.launch

class SkillsDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySkillsDetailsBinding
    private var eventId: Int = -1
    private var currentTeamId: Int = -1
    private var fallbackRegisteredTeams: List<TeamSkillSummary> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySkillsDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applyBottomSystemInsetPadding()

        eventId = intent.getIntExtra("EVENT_ID", -1)
        val sharedPref = getSharedPreferences("VexPrefs", MODE_PRIVATE)
        currentTeamId = sharedPref.getInt("team_id", -1)

        binding.backButton.setOnClickListener { finish() }
        binding.navHomeButton.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            })
            finish()
        }
        binding.changeTeamButton.setOnClickListener {
            getSharedPreferences("VexPrefs", MODE_PRIVATE).edit {
                remove("team_number")
                remove("team_id")
            }
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        binding.skillsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.teamFilterClearButton.setOnClickListener { binding.teamFilterInput.setText("") }
        binding.teamFilterInput.doAfterTextChanged {
            val hasText = !it.isNullOrEmpty()
            binding.teamFilterClearButton.visibility = if (hasText) View.VISIBLE else View.GONE
            if (binding.registeredTeamsTools.visibility == View.VISIBLE) {
                applyRegisteredTeamFilter()
            }
        }

        if (eventId != -1) {
            fetchFullLeaderboard()
        }
    }

    private suspend fun fetchAllEventSkills(eventId: Int): List<CompSkillData> {
        val allSkills = mutableListOf<CompSkillData>()
        var page = 1
        var totalRows = Int.MAX_VALUE

        while (allSkills.size < totalRows) {
            val response = RetrofitClient.service.getEventSkills(eventId, page = page)
            if (!response.isSuccessful) break

            val body = response.body() ?: break
            if (body.data.isEmpty()) break

            allSkills.addAll(body.data)
            totalRows = body.meta?.total ?: allSkills.size

            val currentPage = body.meta?.currentPage ?: page
            val lastPage = body.meta?.lastPage ?: currentPage
            if (currentPage >= lastPage) break

            page++
        }

        return allSkills
    }

    private fun fetchFullLeaderboard() {
        binding.progressBar.visibility = android.view.View.VISIBLE

        lifecycleScope.launch {
            try {
                val rawData = fetchAllEventSkills(eventId)
                processAndDisplaySkills(rawData)
            } catch (e: Exception) {
                Log.e("SKILLS_DEBUG", "Failed to fetch leaderboard", e)
                processAndDisplaySkills(emptyList())
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }

    private fun processAndDisplaySkills(data: List<CompSkillData>) {
        @Suppress("UNCHECKED_CAST")
        val teamNameMap = intent.getSerializableExtra("TEAM_MAP") as? HashMap<Int, String> ?: hashMapOf()

        val summaries: List<TeamSkillSummary>

        if (data.isEmpty()) {
            binding.titleText.text = getString(R.string.registered_teams)
            binding.registeredTeamsTools.visibility = View.VISIBLE

            fallbackRegisteredTeams = teamNameMap.map { (teamId, mapValue) ->
                val parts = mapValue.split("|")
                val number = parts.getOrElse(0) { "" }
                val name = parts.getOrElse(1) { mapValue }

                TeamSkillSummary(
                    teamId = teamId,
                    teamNumber = number,
                    driver = 0,
                    programming = 0,
                    total = 0,
                    rank = 0,
                    teamName = name
                )
            }.sortedWith { left, right ->
                val numberCompare = compareTeamNumbersNatural(left.teamNumber, right.teamNumber)
                if (numberCompare != 0) numberCompare
                else left.teamName.compareTo(right.teamName, ignoreCase = true)
            }

            applyRegisteredTeamFilter()
            return
        } else {
            binding.registeredTeamsTools.visibility = View.GONE
            if (binding.teamFilterInput.text?.isNotEmpty() == true) {
                binding.teamFilterInput.setText("")
            }
            binding.teamFilterClearButton.visibility = View.GONE
            fallbackRegisteredTeams = emptyList()

            summaries = data.groupBy { it.team?.id ?: -1 }
                .filter { it.key != -1 }
                .map { entry ->
                    val teamId = entry.key
                    val firstEntry = entry.value.first()
                    val teamNumber = firstEntry.team?.name ?: "????"

                    val mapValue = teamNameMap[teamId] ?: ""
                    val teamName = if (mapValue.contains("|")) mapValue.split("|")[1] else mapValue

                    val driver = entry.value.filter { it.type == "driver" }.maxOfOrNull { it.score } ?: 0
                    val prog = entry.value.filter { it.type == "programming" }.maxOfOrNull { it.score } ?: 0
                    val rank = entry.value.map { it.rank }.firstOrNull { it > 0 } ?: 0

                    TeamSkillSummary(teamId, teamNumber, driver, prog, driver + prog, rank, teamName)
                }
                .sortedBy { it.rank }
        }

        binding.skillsRecyclerView.adapter = SkillsLeaderboardAdapter(summaries, currentTeamId, teamNameMap)
    }

    private fun applyRegisteredTeamFilter() {
        @Suppress("UNCHECKED_CAST")
        val teamNameMap = intent.getSerializableExtra("TEAM_MAP") as? HashMap<Int, String> ?: hashMapOf()

        val query = binding.teamFilterInput.text?.toString()?.trim().orEmpty()
        val filtered = if (query.isEmpty()) {
            fallbackRegisteredTeams
        } else {
            fallbackRegisteredTeams.filter { summary ->
                summary.teamNumber.contains(query, ignoreCase = true) ||
                    summary.teamName.contains(query, ignoreCase = true)
            }
        }.mapIndexed { index, summary ->
            summary.copy(rank = index + 1)
        }

        binding.registeredTeamsCountText.text = if (query.isEmpty()) {
            getString(R.string.registered_teams_count, fallbackRegisteredTeams.size)
        } else {
            getString(R.string.registered_teams_filtered_count, filtered.size, fallbackRegisteredTeams.size)
        }

        binding.skillsRecyclerView.adapter = SkillsLeaderboardAdapter(filtered, currentTeamId, teamNameMap)
    }
}