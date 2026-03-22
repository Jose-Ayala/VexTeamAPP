package com.jayala.vexapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jayala.vexapp.databinding.ActivitySkillsDetailsBinding
import kotlinx.coroutines.launch

class AwardDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySkillsDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySkillsDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applyBottomSystemInsetPadding()

        val eventId = intent.getIntExtra("EVENT_ID", -1)

        binding.titleText.text = getString(R.string.event_awards)
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

        if (eventId != -1) {
            fetchAwards(eventId)
        }
    }

    private fun fetchAwards(eventId: Int) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                @Suppress("UNCHECKED_CAST")
                val teamNameMap = intent.getSerializableExtra("TEAM_MAP") as? HashMap<Int, String> ?: hashMapOf()

                val response = RetrofitClient.service.getEventAwards(eventId)
                if (response.isSuccessful && response.body() != null) {
                    val awardList = response.body()!!.data

                    val sharedPref = getSharedPreferences("VexPrefs", MODE_PRIVATE)
                    val myTeamId = sharedPref.getInt("team_id", -1)

                    val cleanedTeamMap = teamNameMap.mapValues { (_, value) ->
                        if (value.contains("|")) value.split("|").getOrElse(1) { "" } else value
                    }

                    binding.skillsRecyclerView.adapter = AwardRecipientsAdapter(awardList, myTeamId, cleanedTeamMap)
                }
            } catch (e: Exception) {
                Log.e("AWARD_DEBUG", "Error fetching awards", e)
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
}