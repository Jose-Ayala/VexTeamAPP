package com.jayala.vexapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jayala.vexapp.databinding.ActivitySkillsDetailsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MatchResultsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySkillsDetailsBinding
    private var eventId: Int = -1
    private var filterTeamId: Int? = null // null means show all teams
    private var allDivisions: List<Pair<Division, List<CompMatchData>>> = emptyList()
    private var currentTeamId: Int = -1
    private var teamNameMap: HashMap<Int, String> = hashMapOf()

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
        binding.root.applyBottomSystemInsetPadding()

        binding.titleText.text = getString(R.string.match_results)
        eventId = intent.getIntExtra("EVENT_ID", -1)

        @Suppress("UNCHECKED_CAST")
        teamNameMap = intent.getSerializableExtra("TEAM_MAP") as? HashMap<Int, String> ?: hashMapOf()

        val sharedPref = getSharedPreferences("VexPrefs", MODE_PRIVATE)
        currentTeamId = sharedPref.getInt("team_id", -1)

        // Default filter to current team
        filterTeamId = currentTeamId

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

        // Show filter icon and include it in TalkBack focus on this screen only.
        binding.filterIcon.visibility = View.VISIBLE
        binding.filterIcon.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        binding.filterIcon.setOnClickListener { showFilterDialog() }

        if (eventId != -1) {
            fetchMatchResults()
        }
    }

    private suspend fun fetchAllDivisionMatches(eventId: Int, divisionId: Int): List<CompMatchData> {
        val allMatches = mutableListOf<CompMatchData>()
        var page = 1
        var totalRows = Int.MAX_VALUE

        while (allMatches.size < totalRows) {
            val response = RetrofitClient.service.getEventMatchesByDivisionPath(eventId, divisionId, page = page)
            if (!response.isSuccessful) break

            val body = response.body() ?: break
            if (body.data.isEmpty()) break

            allMatches.addAll(body.data)
            totalRows = body.meta?.total ?: allMatches.size

            val currentPage = body.meta?.currentPage ?: page
            val lastPage = body.meta?.lastPage ?: currentPage
            if (currentPage >= lastPage) break

            page++
        }

        return allMatches.distinctBy { it.id }
    }

    private fun fetchMatchResults() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val eventResponse = RetrofitClient.service.getEventDetails(eventId)
                if (eventResponse.isSuccessful && eventResponse.body() != null) {
                    val divisions = eventResponse.body()?.divisions ?: emptyList()

                    allDivisions = divisions.map { division ->
                        val matches = fetchAllDivisionMatches(eventId, division.id)
                        division to matches
                    }

                    applyFilter()
                }
            } catch (e: Exception) {
                Log.e("MATCH_DEBUG", "Error: ${e.message}")
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun applyFilter() {
        val filteredDivisions = if (filterTeamId != null) {
            // Filter matches to only those containing the selected team
            allDivisions.map { (division, matches) ->
                val filteredMatches = matches.filter { match ->
                    match.alliances.any { alliance ->
                        alliance.teams.any { it.team.id == filterTeamId }
                    }
                }
                division to filteredMatches
            }
        } else {
            // Show all matches
            allDivisions
        }

        val sortedDivisions = filteredDivisions.sortedByDescending { pair ->
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
            binding.emptyStateMessage.text = getString(R.string.no_match_results_available)
            binding.emptyStateMessage.visibility = View.VISIBLE
            binding.skillsRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateMessage.visibility = View.GONE
            binding.skillsRecyclerView.visibility = View.VISIBLE
            binding.skillsRecyclerView.adapter = MatchResultsAdapter(displayList, currentTeamId)
        }
    }

    private fun showFilterDialog() {
        val teams = teamNameMap.map { (id, mapValue) ->
            val parts = mapValue.split("|")
            val number = parts.getOrElse(0) { "" }
            val name = parts.getOrElse(1) { mapValue }
            MatchFilterTeam(id = id, number = number, name = name)
        }.sortedWith { left, right ->
            val numberCompare = compareTeamNumbersNatural(left.number, right.number)
            if (numberCompare != 0) numberCompare
            else left.name.compareTo(right.name, ignoreCase = true)
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_match_results_filter, null)
        val myTeamRow = dialogView.findViewById<TextView>(R.id.myTeamRow)
        val allTeamsRow = dialogView.findViewById<TextView>(R.id.allTeamsRow)
        val teamsRecyclerView = dialogView.findViewById<RecyclerView>(R.id.teamsRecyclerView)
        val closeButton = dialogView.findViewById<Button>(R.id.closeButton)

        val dialog = MaterialAlertDialogBuilder(this).setView(dialogView).create()

        fun updateFixedRowStyles() {
            val myTeamColor = if (filterTeamId == currentTeamId) R.color.accent_gold else R.color.white
            val allTeamsColor = if (filterTeamId == null) R.color.accent_gold else R.color.white
            myTeamRow.setTextColor(ContextCompat.getColor(this, myTeamColor))
            allTeamsRow.setTextColor(ContextCompat.getColor(this, allTeamsColor))
        }

        updateFixedRowStyles()

        teamsRecyclerView.layoutManager = LinearLayoutManager(this)
        teamsRecyclerView.adapter = MatchFilterTeamsAdapter(
            teams = teams,
            selectedTeamId = filterTeamId,
            onTeamSelected = { selectedId ->
                filterTeamId = selectedId
                applyFilter()
                dialog.dismiss()
            }
        )

        myTeamRow.setOnClickListener {
            filterTeamId = currentTeamId
            applyFilter()
            dialog.dismiss()
        }

        allTeamsRow.setOnClickListener {
            filterTeamId = null
            applyFilter()
            dialog.dismiss()
        }

        closeButton.setOnClickListener { dialog.dismiss() }

        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}