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

class MatchRankDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySkillsDetailsBinding
    private var eventId: Int = -1
    private var divisionId: Int = 1
    private var currentTeamId: Int = -1
    private var fallbackRegisteredTeams: List<Triple<String, String, Int>> = emptyList()
    private var cleanedTeamMap: Map<Int, String> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySkillsDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applyBottomSystemInsetPadding()

        binding.titleText.text = getString(R.string.match_rankings)

        eventId = intent.getIntExtra("EVENT_ID", -1)
        divisionId = intent.getIntExtra("DIVISION_ID", 1)

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
            fetchRankings()
        }
    }

    private suspend fun fetchAllDivisionRankings(eventId: Int, divisionId: Int): List<CompRankingData> {
        val allRankings = mutableListOf<CompRankingData>()
        var page = 1
        var totalRows = Int.MAX_VALUE

        while (allRankings.size < totalRows) {
            val response = RetrofitClient.service.getEventRankings(eventId, divisionId, page = page)
            if (!response.isSuccessful) break

            val body = response.body() ?: break
            if (body.data.isEmpty()) break

            allRankings.addAll(body.data)
            totalRows = body.meta?.total ?: allRankings.size

            val currentPage = body.meta?.currentPage ?: page
            val lastPage = body.meta?.lastPage ?: currentPage
            if (currentPage >= lastPage) break

            page++
        }

        return allRankings.distinctBy { it.team?.id ?: -1 }
    }

    private fun fetchRankings() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                @Suppress("UNCHECKED_CAST")
                val teamNameMap = intent.getSerializableExtra("TEAM_MAP") as? HashMap<Int, String> ?: hashMapOf()
                cleanedTeamMap = teamNameMap.mapValues { (_, value) ->
                    if (value.contains("|")) value.split("|").getOrElse(1) { "" } else value
                }

                val eventResponse = RetrofitClient.service.getEventDetails(eventId)
                if (eventResponse.isSuccessful && eventResponse.body() != null) {
                    val divisions = eventResponse.body()?.divisions ?: emptyList()

                    val allDivisionData = divisions.map { division ->
                        val rankings = fetchAllDivisionRankings(eventId, division.id)
                        division to rankings
                    }

                    val sortedDivisions = allDivisionData.sortedByDescending { (_, rankings) ->
                        rankings.any { it.team?.id == currentTeamId }
                    }

                    val displayList = mutableListOf<MatchRankItem>()

                    for ((division, rankings) in sortedDivisions) {
                        if (rankings.isNotEmpty()) {
                            displayList.add(MatchRankItem.Header(division.name))
                            displayList.addAll(rankings.sortedBy { it.rank }.map { MatchRankItem.Rank(it) })
                        }
                    }

                    if (displayList.isEmpty()) {
                        binding.titleText.text = getString(R.string.registered_teams)
                        binding.registeredTeamsTools.visibility = View.VISIBLE

                        fallbackRegisteredTeams = teamNameMap.map { (teamId, mapValue) ->
                            val parts = mapValue.split("|")
                            val number = parts.getOrElse(0) { "" }
                            val name = parts.getOrElse(1) { mapValue }
                            Triple(number, name, teamId)
                        }.sortedWith { left, right ->
                            val numberCompare = compareTeamNumbersNatural(left.first, right.first)
                            if (numberCompare != 0) numberCompare
                            else left.second.compareTo(right.second, ignoreCase = true)
                        }

                        applyRegisteredTeamFilter()
                        return@launch
                    }

                    binding.registeredTeamsTools.visibility = View.GONE
                    if (binding.teamFilterInput.text?.isNotEmpty() == true) {
                        binding.teamFilterInput.setText("")
                    }
                    binding.teamFilterClearButton.visibility = View.GONE
                    fallbackRegisteredTeams = emptyList()

                    binding.skillsRecyclerView.adapter = MatchRankLeaderboardAdapter(displayList, currentTeamId, cleanedTeamMap)
                }
            } catch (e: Exception) {
                Log.e("RANK_DEBUG", "Failed to fetch rankings", e)
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun applyRegisteredTeamFilter() {
        val query = binding.teamFilterInput.text?.toString()?.trim().orEmpty()
        val filtered = if (query.isEmpty()) {
            fallbackRegisteredTeams
        } else {
            fallbackRegisteredTeams.filter { row ->
                row.first.contains(query, ignoreCase = true) ||
                    row.second.contains(query, ignoreCase = true)
            }
        }

        binding.registeredTeamsCountText.text = if (query.isEmpty()) {
            getString(R.string.registered_teams_count, fallbackRegisteredTeams.size)
        } else {
            getString(R.string.registered_teams_filtered_count, filtered.size, fallbackRegisteredTeams.size)
        }

        val fallbackItems = filtered.mapIndexed { index, row ->
            MatchRankItem.Rank(
                CompRankingData(
                    rank = index + 1,
                    team = TeamRef(id = row.third, name = row.first, code = null),
                    wins = 0,
                    losses = 0,
                    ties = 0,
                    wp = 0,
                    ap = 0,
                    sp = 0,
                    event = null
                )
            )
        }

        binding.skillsRecyclerView.adapter = MatchRankLeaderboardAdapter(fallbackItems, currentTeamId, cleanedTeamMap)
    }
}