package com.jayala.vexapp

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jayala.vexapp.databinding.ActivityCompsBinding
import kotlinx.coroutines.launch
import android.widget.LinearLayout
import android.widget.ImageView
import android.view.Gravity
import com.google.android.material.card.MaterialCardView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import kotlin.jvm.java

class CompsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompsBinding
    private var eventList: List<CompEventDetail> = listOf()
    private var teamNameMap = mutableMapOf<Int, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applyBottomSystemInsetPadding()

        val sharedPref = getSharedPreferences("VexPrefs", MODE_PRIVATE)
        val teamId = sharedPref.getInt("team_id", -1)
        val teamName = sharedPref.getString("team_full_name", "Team Info")
        binding.teamName.text = teamName

        binding.backButton.setOnClickListener { finish() }
        binding.navHomeButton.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            })
            finish()
        }
        binding.changeTeamButton.setOnClickListener {
            sharedPref.edit {
                remove("team_number")
                remove("team_id")
            }
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        if (teamId != -1) {
            loadDropdownData(teamId)
        }
    }

    private fun formatDate(rawDate: String?): String {
        if (rawDate.isNullOrEmpty() || rawDate.length < 10) return "Date TBD"
        return try {
            val year = rawDate.substring(0, 4)
            val month = rawDate.substring(5, 7)
            val day = rawDate.substring(8, 10)
            "$month-$day-$year"
        } catch (_: Exception) {
            "Date TBD"
        }
    }

    private fun loadDropdownData(teamId: Int) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE

                val seasonsResp = RetrofitClient.service.getSeasons(active = true)
                val activeSeasonIds = seasonsResp.body()?.data?.map { it.id } ?: emptyList()

                val response = RetrofitClient.service.getCompEvents(teamId)

                if (response.isSuccessful && response.body() != null) {
                    eventList = response.body()!!.data.filter { event ->
                        activeSeasonIds.contains(event.season?.id)
                    }.reversed()

                    val today = java.time.LocalDate.now()
                    var preselectedIndex = -1
                    val formattedNames = mutableListOf<CharSequence>()

                    eventList.forEachIndexed { index, event ->
                        val dateStr = formatDate(event.start)
                        val shortName = event.name.substringBefore(":").trim()
                        val fullText = "$shortName - $dateStr"

                        val spannable = android.text.SpannableString(fullText)
                        val dateStartIndex = fullText.lastIndexOf(dateStr)
                        if (dateStartIndex != -1) {
                            spannable.setSpan(
                                ForegroundColorSpan(ContextCompat.getColor(this@CompsActivity, R.color.accent_gold)),
                                dateStartIndex,
                                fullText.length,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                        formattedNames.add(spannable)

                        try {
                            val eventDate = java.time.LocalDate.parse(event.start?.substring(0, 10))
                            if (preselectedIndex == -1 && !eventDate.isAfter(today)) {
                                preselectedIndex = index
                            }
                        } catch (e: Exception) {
                            Log.e("COMP_DEBUG", "Date parse error", e)
                        }
                    }

                    val adapter = ArrayAdapter<CharSequence>(this@CompsActivity, R.layout.spinner_item, formattedNames)
                    adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                    binding.competitionDropdown.adapter = adapter

                    if (eventList.isNotEmpty()) {
                        val finalSelection = if (preselectedIndex >= 0) preselectedIndex else 0
                        binding.competitionDropdown.setSelection(finalSelection)
                        val selectedEvent = eventList[finalSelection]
                        fetchEventDetails(teamId, selectedEvent.id, formatDate(selectedEvent.start))
                    }

                    setupDropdownListener(teamId)
                }
            } catch (e: Exception) {
                Log.e("COMP_DEBUG", "Dropdown error", e)
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun setupDropdownListener(teamId: Int) {
        binding.competitionDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                if (pos in eventList.indices) {
                    val selectedEvent = eventList[pos]
                    fetchEventDetails(teamId, selectedEvent.id, formatDate(selectedEvent.start))
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private suspend fun fetchAllEventTeams(eventId: Int): List<EventTeamData> {
        val allTeams = mutableListOf<EventTeamData>()
        var page = 1
        var totalTeams = Int.MAX_VALUE

        while (allTeams.size < totalTeams) {
            val response = RetrofitClient.service.getEventTeams(eventId, page = page)
            if (!response.isSuccessful) break

            val body = response.body() ?: break
            if (body.data.isEmpty()) break

            allTeams.addAll(body.data)
            totalTeams = body.meta?.total ?: allTeams.size

            val currentPage = body.meta?.currentPage ?: page
            val lastPage = body.meta?.lastPage ?: currentPage
            if (currentPage >= lastPage) break

            page++
        }

        return allTeams.distinctBy { it.id }
    }

    private fun fetchEventDetails(teamId: Int, eventId: Int, formattedDate: String) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.contentScroll.visibility = View.GONE

                teamNameMap.clear()
                fetchAllEventTeams(eventId).forEach { team ->
                    val combinedName = "${team.number}|${team.teamName ?: ""}"
                    teamNameMap[team.id] = combinedName
                }

                val seasons = (180..215).toList()
                val sRes = RetrofitClient.service.getCompSkills(teamId, seasons)
                val rRes = RetrofitClient.service.getCompRankings(teamId, seasons)

                val aRes = RetrofitClient.service.getEventAwards(eventId)

                if (sRes.isSuccessful) {
                    val eventSkills = sRes.body()?.data?.filter { it.event?.id == eventId } ?: emptyList()
                    val rank = rRes.body()?.data?.find { it.event?.id == eventId }

                    val awards = if (aRes.isSuccessful) {
                        aRes.body()?.data?.filter { award ->
                            award.teamWinners?.any { it.team?.id == teamId } == true
                        } ?: emptyList()
                    } else emptyList()

                    val finalSkillsRank = eventSkills.firstOrNull()?.rank ?: 0
                    val selectedEvent = eventList.find { it.id == eventId }

                    updateUI(eventSkills, rank, awards, formattedDate, finalSkillsRank, selectedEvent?.location, eventId)
                }
            } catch (e: Exception) {
                Log.e("COMP_DEBUG", "Fetch error", e)
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.contentScroll.visibility = View.VISIBLE
            }
        }
    }

    private fun updateUI(
        skills: List<CompSkillData>,
        rank: CompRankingData?,
        awards: List<CompAwardData>,
        eventDate: String,
        skillsRank: Int,
        eventLocation: Location?,
        eventId: Int
    ) {
        binding.detailsContainer.removeAllViews()

        fun addSection(
            title: String,
            body: String,
            iconRes: Int,
            secondaryText: String? = null,
            isSkills: Boolean = false,
            onClick: (() -> Unit)? = null
        ) {
            val card = MaterialCardView(this).apply {
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 40)
                layoutParams = params

                radius = 45f
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface_dark))
                strokeColor = ContextCompat.getColor(context, R.color.divider_dark)
                strokeWidth = 3
                cardElevation = 0f

                if (onClick != null) {
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { onClick() }
                }
            }

            val horizontalLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(50, 50, 50, 50)
                gravity = Gravity.CENTER_VERTICAL
            }

            val icon = ImageView(this).apply {
                val iconParams = LinearLayout.LayoutParams(100, 100)
                iconParams.setMargins(0, 0, 40, 0)
                layoutParams = iconParams
                setImageResource(iconRes)
            }

            val textLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val header = TextView(this).apply {
                text = title
                textSize = 18f
                setTextColor(Color.WHITE)
                setTypeface(null, Typeface.BOLD)
            }
            textLayout.addView(header)

            if (isSkills) {
                val parts = body.split("\n")
                val topInfo = TextView(this).apply {
                    text = parts[0]
                    textSize = 14f
                    setTextColor("#00D2FF".toColorInt())
                    setPadding(0, 10, 0, 0)
                }
                val totalInfo = TextView(this).apply {
                    text = parts.getOrNull(1) ?: ""
                    textSize = 14f
                    setTextColor(ContextCompat.getColor(context, R.color.accent_gold))
                    setPadding(0, 5, 0, 0)
                }
                textLayout.addView(topInfo)
                textLayout.addView(totalInfo)

                parts.getOrNull(2)?.let { rankLine ->
                    val rankInfo = TextView(this).apply {
                        text = rankLine
                        textSize = 14f
                        setTextColor("#4CAF50".toColorInt())
                        setPadding(0, 5, 0, 0)
                    }
                    textLayout.addView(rankInfo)
                }
            } else {
                val content = TextView(this).apply {
                    textSize = 14f
                    setPadding(0, 10, 0, 0)

                    if ((title == "Venue" || title == getString(R.string.competitions)
                                || title == getString(R.string.match_rankings) || title == getString(R.string.match_results)) && body.contains("\n")) {
                        val parts = body.split("\n")
                        val rankLine = parts[0]
                        val recordLine = parts[1]
                        val statsLine = parts.getOrNull(2) ?: ""

                        val builder = SpannableStringBuilder()
                        builder.append(rankLine)
                        builder.setSpan(ForegroundColorSpan("#4CAF50".toColorInt()), 0, rankLine.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                        builder.append("\n")
                        val recordStart = builder.length
                        builder.append(recordLine)
                        builder.setSpan(ForegroundColorSpan("#00D2FF".toColorInt()), recordStart, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                        if (statsLine.isNotEmpty()) {
                            builder.append("\n")
                            val statsStart = builder.length
                            builder.append(statsLine)
                            builder.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.accent_gold)), statsStart, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        setText(builder, TextView.BufferType.SPANNABLE)
                    } else {
                        text = body
                        setTextColor("#00D2FF".toColorInt())
                    }
                }
                textLayout.addView(content)
            }

            if (secondaryText != null) {
                val dateView = TextView(this).apply {
                    text = secondaryText
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(context, R.color.white))
                    setPadding(0, 20, 0, 0)
                }
                textLayout.addView(dateView)
            }

            horizontalLayout.addView(icon)
            horizontalLayout.addView(textLayout)

            if (onClick != null) {
                val chevron = ImageView(this).apply {
                    val chevParams = LinearLayout.LayoutParams(60, 60)
                    layoutParams = chevParams
                    setImageResource(R.drawable.ic_chevron_right)
                    setColorFilter(Color.GRAY)
                }
                horizontalLayout.addView(chevron)
            }

            card.addView(horizontalLayout)
            binding.detailsContainer.addView(card)
        }

        eventLocation?.let { loc ->
            val aboutBody = SpannableStringBuilder().apply {
                val venueText = loc.venue ?: "Unknown Venue"
                append(venueText)
                setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(this@CompsActivity, R.color.cyber_blue)),
                    0,
                    venueText.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                append("\n")
                val startOfLocation = length
                val locationLine = "${loc.address_1 ?: ""}, ${loc.region ?: ""}"
                append(locationLine)

                setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(this@CompsActivity, R.color.skills_rank_green)),
                    startOfLocation,
                    length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            addSection("Venue", aboutBody.toString(), R.drawable.ic_info)
        }

        val d = skills.filter { it.type.equals("driver", true) }.maxOfOrNull { it.score } ?: 0
        val p = skills.filter { it.type.equals("programming", true) }.maxOfOrNull { it.score } ?: 0
        val total = d + p

        val skillsBody = StringBuilder("Driver: $d | Prog: $p\nTotal: $total")
        if (skillsRank > 0) {
            skillsBody.append("\nRank: $skillsRank")
        }

        addSection(
            title = getString(R.string.skills),
            body = skillsBody.toString(),
            iconRes = R.drawable.ic_skills,
            isSkills = true,
            onClick = {
                val intent = Intent(this@CompsActivity, SkillsDetailsActivity::class.java)
                intent.putExtra("EVENT_ID", eventId)
                intent.putExtra("TEAM_MAP", HashMap(teamNameMap))
                startActivity(intent)
            }
        )

        val rankText = rank?.let {
            "Rank: ${it.rank}\n" +
                    "Record: ${it.wins}W - ${it.losses}L - ${it.ties}T\n" +
                    "WP: ${it.wp} | AP: ${it.ap} | SP: ${it.sp}"
        } ?: "No ranking data available."

        addSection(getString(R.string.match_rankings), rankText, R.drawable.ic_leaderboard, onClick = {
            val intent = Intent(this@CompsActivity, MatchRankDetailsActivity::class.java)
            intent.putExtra("EVENT_ID", eventId)
            intent.putExtra("TEAM_MAP", HashMap(teamNameMap))
            startActivity(intent)
        })

        addSection(
            title = getString(R.string.match_results),
            body = "Schedule and Results",
            iconRes = R.drawable.ic_calendar,
            onClick = {
                val intent = Intent(this@CompsActivity, MatchResultsActivity::class.java)
                intent.putExtra("EVENT_ID", eventId)
                intent.putExtra("TEAM_MAP", HashMap(teamNameMap))
                startActivity(intent)
            }
        )

        val awardText = if (awards.isEmpty()) {
            "No awards won."
        } else {
            awards.joinToString("\n") { award ->
                award.title.substringBefore("(").trim()
            }
        }

        addSection(
            title = getString(R.string.awards),
            body = awardText,
            iconRes = R.drawable.ic_trophy,
            onClick = {
                val intent = Intent(this@CompsActivity, AwardDetailsActivity::class.java)
                intent.putExtra("EVENT_ID", eventId)
                intent.putExtra("EVENT_NAME", eventList.find { it.id == eventId }?.name)
                intent.putExtra("TEAM_MAP", HashMap(teamNameMap))
                startActivity(intent)
            }
        )
    }
}
