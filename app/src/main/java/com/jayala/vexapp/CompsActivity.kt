package com.jayala.vexapp

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jayala.vexapp.databinding.ActivityCompsBinding
import kotlinx.coroutines.launch

class CompsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompsBinding
    // Uses the isolated model from CompModels.kt
    private var eventList: List<CompEventDetail> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val teamId = intent.getIntExtra("TEAM_ID", -1)

        // Button click listener for the HOME button at the bottom
        binding.backButton.setOnClickListener {
            finish()
        }

        if (teamId != -1) {
            loadDropdownData(teamId)
        }
    }

    private fun loadDropdownData(teamId: Int) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.service.getCompEvents(teamId)
                if (response.isSuccessful && response.body() != null) {
                    eventList = response.body()!!.data.reversed()

                    val names = mutableListOf("Select an Event")
                    // This now resolves because CompEventDetail has 'name'
                    names.addAll(eventList.map { it.name })

                    val adapter = ArrayAdapter(
                        this@CompsActivity,
                        R.layout.spinner_item,
                        names
                    )

                    adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                    binding.competitionDropdown.adapter = adapter

                    binding.competitionDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                            if (pos > 0) {
                                // Subtract 1 because of the "Select an Event" header
                                val selectedEventId = eventList[pos - 1].id
                                fetchEventDetails(teamId, selectedEventId)
                            } else {
                                binding.detailsContainer.removeAllViews()
                            }
                        }
                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun fetchEventDetails(teamId: Int, eventId: Int) {
        lifecycleScope.launch {
            try {
                val seasons = (180..210).toList()
                val sRes = RetrofitClient.service.getCompSkills(teamId, seasons)
                val rRes = RetrofitClient.service.getCompRankings(teamId, seasons)
                val aRes = RetrofitClient.service.getCompAwards(teamId, seasons)

                if (sRes.isSuccessful) {
                    val allSkills = sRes.body()?.data ?: emptyList()
                    val skills = allSkills.filter { it.event?.id == eventId }
                    val rank = rRes.body()?.data?.find { it.event?.id == eventId }
                    val awards = aRes.body()?.data?.filter { it.event?.id == eventId } ?: emptyList()

                    updateUI(skills, rank, awards)
                }
            } catch (e: Exception) {
                android.util.Log.e("COMP_DEBUG", "Error fetching details", e)
            }
        }
    }

    private fun updateUI(skills: List<CompSkillData>, rank: CompRankingData?, awards: List<CompAwardData>) {
        binding.detailsContainer.removeAllViews()

        fun addSection(title: String, body: String) {
            val header = TextView(this).apply {
                text = title
                textSize = 18f
                setTextColor(Color.WHITE)
                setTypeface(null, Typeface.BOLD)
                setPadding(0, 20, 0, 5)
            }
            val content = TextView(this).apply {
                text = body
                textSize = 15f
                setTextColor(Color.LTGRAY)
                setPadding(0, 0, 0, 10)
            }
            binding.detailsContainer.addView(header)
            binding.detailsContainer.addView(content)
        }

        val driver = skills.filter { it.type.equals("driver", ignoreCase = true) }
            .maxOfOrNull { it.score } ?: 0
        val prog = skills.filter { it.type.equals("programming", ignoreCase = true) }
            .maxOfOrNull { it.score } ?: 0
        addSection("Skills", "Driver: $driver\nProgramming: $prog\nTotal: ${driver + prog}")

        rank?.let {
            addSection("Rankings", "Rank: ${it.rank}\nRecord: ${it.wins}W - ${it.losses}L - ${it.ties}T")
        } ?: addSection("Rankings", "No ranking data available for this event.")

        val awardText = if (awards.isEmpty()) "No awards won at this event."
        else awards.joinToString("\n") { "• ${it.title}" }
        addSection("Awards", awardText)
    }
}