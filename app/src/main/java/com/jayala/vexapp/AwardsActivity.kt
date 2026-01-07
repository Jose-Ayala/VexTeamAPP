package com.jayala.vexapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jayala.vexapp.databinding.ActivityAwardsBinding
import kotlinx.coroutines.launch

class AwardsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAwardsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAwardsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }

        val teamId = intent.getIntExtra("TEAM_ID", -1)
        if (teamId != -1) {
            fetchAwardsData(teamId)
        }
    }

    private fun fetchAwardsData(teamId: Int) {
        lifecycleScope.launch {
            try {
                val startDate = "2023-08-01T00:00:00Z"
                val seasons = (181..202).toList()

                val eventsResponse = RetrofitClient.service.getTeamEvents(teamId, startDate = startDate)
                val awardsResponse = RetrofitClient.service.getTeamAwards(teamId, seasons)

                if (eventsResponse.isSuccessful && awardsResponse.isSuccessful) {
                    val eventsData = eventsResponse.body()?.data ?: emptyList()
                    val awardsData = awardsResponse.body()?.data ?: emptyList()

                    val eventDateMap = eventsData.associate { it.id to it.start }

                    val uiModels = awardsData.map { award: AwardData ->
                        val rawDate = eventDateMap[award.event?.id] ?: ""
                        val cleanedTitle = award.title.replace(Regex("\\s*\\(.*?\\)\\s*"), "").trim()

                        val displayDate = if (rawDate.isNotEmpty()) {
                            val parts = rawDate.split("T")[0].split("-")
                            if (parts.size == 3) {
                                "${parts[1]}/${parts[2]}/${parts[0].substring(2)}"
                            } else "TBD"
                        } else "TBD"

                        AwardUiModel(
                            title = cleanedTitle,
                            eventName = award.event?.name ?: "Unknown Event",
                            displayDate = displayDate,
                            sortableDate = rawDate
                        )
                    }.sortedByDescending { it.sortableDate }

                    val totalCount = uiModels.size
                    binding.awardsSubtitle.text = getString(R.string.trophies_and_achievements_count, totalCount)

                    binding.awardsRecyclerView.adapter = AwardsAdapter(uiModels)

                    if (uiModels.isEmpty()) {
                        Toast.makeText(this@AwardsActivity, "No awards found for this team.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("VEX_DEBUG", "API Error - Events: ${eventsResponse.code()}, Awards: ${awardsResponse.code()}")
                }
            } catch (e: Exception) {
                Log.e("VEX_DEBUG", "Critical Fetch Failure", e)
                Toast.makeText(this@AwardsActivity, "Error loading awards", Toast.LENGTH_SHORT).show()
            }
        }
    }
}