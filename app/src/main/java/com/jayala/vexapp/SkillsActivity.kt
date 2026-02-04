package com.jayala.vexapp

import com.jayala.vexapp.SkillsMarkerView
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jayala.vexapp.databinding.ActivitySkillsBinding
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.components.XAxis

class SkillsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySkillsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySkillsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPref = getSharedPreferences("VexPrefs", MODE_PRIVATE)
        val teamId = sharedPref.getInt("team_id", -1)
        val teamName = sharedPref.getString("team_full_name", "Team Info")
        binding.teamName.text = teamName

        if (teamId != -1) {
            fetchSkillsData(teamId)
        } else {
            showError(getString(R.string.err_network))
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.retryButton.setOnClickListener {
            if (teamId != -1) fetchSkillsData(teamId)
        }
    }

    private fun fetchSkillsData(teamId: Int) {
        lifecycleScope.launch {
            setLoading(true)
            try {
                Log.d("VEX_DEBUG", "Fetching skills for Team ID: $teamId")

                // Range covering recent seasons since 2023
                val allSeasonIds = (180..215).toList()
                val response = RetrofitClient.service.getTeamSkills(
                    teamId = teamId,
                    seasons = allSeasonIds
                )

                if (response.isSuccessful && response.body() != null) {
                    val rawData = response.body()!!.data

                    // 1. Group raw data by Event and Season to merge Driver/Prog scores
                    val groupedByEvent = rawData.groupBy { dataItem: SkillsData ->
                        dataItem.event.name to dataItem.season.name
                    }

                    // 2. Create the list of event models, reversed for newest first
                    val uiModels = groupedByEvent.map { (key, entries) ->
                        val (eventName, rawSeasonName) = key

                        // Combine V5 and VEX U under a unified name, but keep others (like IQ) as-is
                        val isV5orU = rawSeasonName.contains("V5", ignoreCase = true) ||
                                rawSeasonName.contains("VEX U", ignoreCase = true)

                        val finalSeasonName = if (isV5orU) {
                            rawSeasonName
                                .replace("VEX U Robotics Competition ", "", ignoreCase = true)
                                .replace("VEX V5 Robotics Competition ", "", ignoreCase = true)
                                .replace("V5RC ", "", ignoreCase = true)
                                .trim()
                        } else {
                            rawSeasonName
                        }

                        val driver = entries.find { it.type.equals("driver", true) }?.score ?: 0
                        val programming = entries.find { it.type.equals("programming", true) }?.score ?: 0
                        val rank = entries.firstOrNull()?.rank ?: 0

                        SkillsUiModel(
                            eventName = eventName,
                            seasonName = finalSeasonName,
                            driverScore = driver,
                            programmingScore = programming,
                            totalScore = driver + programming,
                            rank = rank
                        )
                    }.reversed()

                    // --- START CHART LOGIC ---
                    if (uiModels.isNotEmpty()) {
                        // Identify the most recent season (using the normalized name)
                        val currentSeasonName = uiModels.first().seasonName

                        // Filter for only that season's data
                        val currentSeasonModels = uiModels.filter { it.seasonName == currentSeasonName }

                        binding.skillsChart.visibility = View.VISIBLE
                        // Reverse again so chart flows Oldest -> Newest (Left to Right)
                        setupChart(currentSeasonModels.reversed())
                    } else {
                        binding.skillsChart.visibility = View.GONE
                    }
                    // --- END CHART LOGIC ---

                    // 3. Transform the list into Header/Skill objects for the RecyclerView
                    val itemsWithHeaders = mutableListOf<SkillsListItem>()
                    var currentSeason = ""

                    uiModels.forEach { model ->
                        if (model.seasonName != currentSeason) {
                            currentSeason = model.seasonName
                            itemsWithHeaders.add(SkillsListItem.Header(currentSeason))
                        }
                        itemsWithHeaders.add(SkillsListItem.Skill(model))
                    }

                    updateUI(itemsWithHeaders)
                    Log.d("VEX_DEBUG", "Successfully displayed ${itemsWithHeaders.size} items")

                } else {
                    Log.e("VEX_DEBUG", "API Error: ${response.code()}")
                    showError(getString(R.string.err_network))
                }
            } catch (e: Exception) {
                Log.e("VEX_DEBUG", "Skills Fetch Failed", e)
                showError(getString(R.string.check_internet))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun setupChart(models: List<SkillsUiModel>) {
        val chart = binding.skillsChart
        if (models.isEmpty()) return

        val entries = models.mapIndexed { index, model ->
            Entry((index + 1).toFloat(), model.totalScore.toFloat())
        }

        val dataSet = LineDataSet(entries, "Season Progress").apply {
            color = "#00B0FF".toColorInt()
            setCircleColor("#FFC107".toColorInt())
            lineWidth = 3f
            circleRadius = 5f
            setDrawCircleHole(false)
            valueTextColor = android.graphics.Color.WHITE
            valueTextSize = 10f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = "#00B0FF".toColorInt()
            fillAlpha = 40
        }

        chart.apply {
            val marker = SkillsMarkerView(context, R.layout.layout_chart_marker, models)
            marker.chartView = this
            this.marker = marker
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setScaleEnabled(false)
            isDragEnabled = false // Allow NestedScrollView to handle the vertical drag

            xAxis.apply {
                textColor = android.graphics.Color.WHITE
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
            }

            axisLeft.apply {
                textColor = android.graphics.Color.WHITE
                gridColor = "#33FFFFFF".toColorInt()
                setDrawGridLines(true)
            }

            axisRight.isEnabled = false
            data = LineData(dataSet)
            animateX(1000)
            invalidate()
        }
    }

    private fun updateUI(models: List<SkillsListItem>) {
        if (models.isEmpty()) {
            binding.skillsRecyclerView.visibility = View.GONE
            binding.emptyStateText.visibility = View.VISIBLE
        } else {
            binding.skillsRecyclerView.visibility = View.VISIBLE
            binding.emptyStateText.visibility = View.GONE
            binding.skillsRecyclerView.adapter = SkillsAdapter(models)
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            binding.errorLayout.visibility = View.GONE
            binding.skillsRecyclerView.visibility = View.GONE
            binding.emptyStateText.visibility = View.GONE
        }
    }

    private fun showError(message: String) {
        binding.errorLayout.visibility = View.VISIBLE
        binding.errorText.text = message
        binding.skillsRecyclerView.visibility = View.GONE
        binding.emptyStateText.visibility = View.GONE
    }
}

data class SkillsUiModel(
    val eventName: String,
    val seasonName: String,
    val driverScore: Int,
    val programmingScore: Int,
    val totalScore: Int,
    val rank: Int
)

sealed class SkillsListItem {
    data class Header(val seasonName: String) : SkillsListItem()
    data class Skill(val model: SkillsUiModel) : SkillsListItem()
}