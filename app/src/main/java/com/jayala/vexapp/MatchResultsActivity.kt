package com.jayala.vexapp

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jayala.vexapp.databinding.ActivitySkillsDetailsBinding
import kotlinx.coroutines.launch

class MatchResultsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySkillsDetailsBinding
    private var eventId: Int = -1

    private val roundPriority = mapOf(
        2 to 1, // Qualification
        6 to 2, // Round of 16
        3 to 3, // Quarter-Finals
        4 to 4, // Semi-Finals
        5 to 5  // Finals
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySkillsDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.titleText.text = getString(R.string.match_results)
        eventId = intent.getIntExtra("EVENT_ID", -1)

        binding.backButton.setOnClickListener { finish() }
        binding.skillsRecyclerView.layoutManager = LinearLayoutManager(this)

        if (eventId != -1) {
            fetchMatchResults()
        }
    }

    private fun fetchMatchResults() {
        binding.progressBar.visibility = View.VISIBLE

        val sharedPref = getSharedPreferences("VexPrefs", MODE_PRIVATE)
        val currentTeamId = sharedPref.getInt("team_id", -1)

        lifecycleScope.launch {
            try {
                val eventResponse = RetrofitClient.service.getEventDetails(eventId)
                if (eventResponse.isSuccessful && eventResponse.body() != null) {
                    val divisions = eventResponse.body()?.divisions ?: emptyList()

                    val allDivisionData = divisions.map { division ->
                        val matchResponse = RetrofitClient.service.getEventMatchesByDivisionPath(eventId, division.id)
                        val matches = if (matchResponse.isSuccessful) matchResponse.body()?.data ?: emptyList() else emptyList()
                        division to matches
                    }

                    val sortedDivisions = allDivisionData.sortedByDescending { pair ->
                        pair.second.any { match ->
                            match.alliances.any { alliance ->
                                alliance.teams.any { it.team.id == currentTeamId }
                            }
                        }
                    }

                    val displayList = mutableListOf<MatchResultItem>()
                    for ((division, matches) in sortedDivisions) {
                        if (matches.isNotEmpty()) {
                            displayList.add(MatchResultItem.Header(division.name))

                            val sortedMatches = matches.sortedWith(compareBy(
                                { roundPriority[it.round] ?: 99 },
                                { it.matchnum }
                            ))

                            displayList.addAll(sortedMatches.map { MatchResultItem.Match(it) })
                        }
                    }

                    binding.titleText.text = getString(R.string.match_results)

                    if (displayList.isEmpty()) {
                        binding.emptyStateMessage.text = "No Match Results Available"
                        binding.emptyStateMessage.visibility = View.VISIBLE
                        binding.skillsRecyclerView.visibility = View.GONE
                    } else {
                        binding.emptyStateMessage.visibility = View.GONE
                        binding.skillsRecyclerView.visibility = View.VISIBLE
                        binding.skillsRecyclerView.adapter = MatchResultsAdapter(displayList, currentTeamId)
                    }
                }
            } catch (e: Exception) {
                Log.e("MATCH_DEBUG", "Error: ${e.message}")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
}