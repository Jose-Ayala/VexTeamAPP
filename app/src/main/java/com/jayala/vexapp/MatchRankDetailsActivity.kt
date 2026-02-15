package com.jayala.vexapp

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jayala.vexapp.databinding.ActivitySkillsDetailsBinding
import kotlinx.coroutines.launch

class MatchRankDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySkillsDetailsBinding
    private var eventId: Int = -1
    private var divisionId: Int = 1
    private var currentTeamId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySkillsDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.titleText.text = getString(R.string.match_rankings)

        eventId = intent.getIntExtra("EVENT_ID", -1)
        divisionId = intent.getIntExtra("DIVISION_ID", 1)

        val sharedPref = getSharedPreferences("VexPrefs", MODE_PRIVATE)
        currentTeamId = sharedPref.getInt("team_id", -1)

        binding.backButton.setOnClickListener { finish() }
        binding.skillsRecyclerView.layoutManager = LinearLayoutManager(this)

        if (eventId != -1) {
            fetchRankings()
        }
    }

    private fun fetchRankings() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                @Suppress("UNCHECKED_CAST")
                val teamNameMap = intent.getSerializableExtra("TEAM_MAP") as? HashMap<Int, String> ?: hashMapOf()

                val eventResponse = RetrofitClient.service.getEventDetails(eventId)
                if (eventResponse.isSuccessful && eventResponse.body() != null) {
                    val divisions = eventResponse.body()?.divisions ?: emptyList()

                    val allDivisionData = divisions.map { division ->
                        val rankResponse = RetrofitClient.service.getEventRankings(eventId, division.id)
                        val rankings = if (rankResponse.isSuccessful) rankResponse.body()?.data ?: emptyList() else emptyList()
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

                        val fallbackItems = teamNameMap.map { (teamId, mapValue) ->
                            val parts = mapValue.split("|")
                            val number = parts.getOrElse(0) { "" }

                            MatchRankItem.Rank(
                                CompRankingData(
                                    rank = 0,
                                    team = TeamRef(id = teamId, name = number, code = null),
                                    wins = 0, losses = 0, ties = 0,
                                    wp = 0, ap = 0, sp = 0,
                                    event = null
                                )
                            )
                        }.sortedBy { it.data.team?.name?.lowercase() }
                            .mapIndexed { index, item ->
                                val rankData = item.data
                                MatchRankItem.Rank(rankData.copy(rank = index + 1))
                            }

                        displayList.addAll(fallbackItems)
                    }

                    binding.skillsRecyclerView.adapter = MatchRankLeaderboardAdapter(displayList, currentTeamId, teamNameMap)
                }
            } catch (e: Exception) {
                Log.e("RANK_DEBUG", "Failed to fetch rankings", e)
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
}