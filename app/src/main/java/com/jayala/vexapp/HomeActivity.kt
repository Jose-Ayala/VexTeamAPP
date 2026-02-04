package com.jayala.vexapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.jayala.vexapp.databinding.ActivityHomeBinding
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.appupdate.AppUpdateOptions

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private var teamId: Int? = null
    private lateinit var sharedPref: android.content.SharedPreferences

    private lateinit var appUpdateManager: AppUpdateManager
    private val updateType = AppUpdateType.FLEXIBLE

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val bytesDownloaded = state.bytesDownloaded()
                val totalBytesToDownload = state.totalBytesToDownload()
                binding.updateProgressBar.visibility = View.VISIBLE
                binding.updateProgressText.visibility = View.VISIBLE
                if (totalBytesToDownload > 0) {
                    val progress = (bytesDownloaded * 100 / totalBytesToDownload).toInt()
                    binding.updateProgressBar.progress = progress
                    binding.updateProgressText.text = getString(R.string.update_progress_format, progress)
                }
            }
            InstallStatus.DOWNLOADED -> {
                binding.updateProgressBar.visibility = View.GONE
                binding.updateProgressText.visibility = View.GONE
                showThemedUpdateSnackbar()
            }
            InstallStatus.FAILED -> {
                binding.updateProgressBar.visibility = View.GONE
                binding.updateProgressText.text = getString(R.string.update_failed_retrying_soon)
            }
            else -> {}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager.registerListener(installStateUpdatedListener)
        sharedPref = getSharedPreferences("VexPrefs", MODE_PRIVATE)

        val savedNumber = sharedPref.getString("team_number", null)
        val savedId = sharedPref.getInt("team_id", -1)

        setupNavigation()

        if (savedNumber != null && savedId != -1) {
            this.teamId = savedId
            fetchTeamData(savedNumber, savedId)
        } else {
            navigateToMain()
        }

        binding.changeTeamButton.setOnClickListener {
            sharedPref.edit {
                remove("team_number")
                remove("team_id")
            }
            navigateToMain()
        }

        binding.aboutButton.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }

    private fun setupNavigation() {
        setButtonsEnabled(false)
        binding.skillsButton.setOnClickListener { launchSection(SkillsActivity::class.java) }
        binding.competitionsButton.setOnClickListener { launchSection(CompsActivity::class.java) }
        binding.awardsButton.setOnClickListener { launchSection(AwardsActivity::class.java) }
    }

    private fun fetchTeamData(teamNumber: String, id: Int) {
        lifecycleScope.launch {
            try {
                binding.organization.text = getString(R.string.loading_team_info)
                val response = RetrofitClient.service.getTeamInfo(teamNumber = teamNumber)

                if (response.isSuccessful && response.body() != null) {
                    val team = response.body()!!.data.find { it.id == id }
                    if (team != null) {
                        val headerText = getString(R.string.team_name_format, team.number, team.team_name)
                        sharedPref.edit { putString("team_full_name", headerText) }

                        val spannable = android.text.SpannableString(headerText)
                        val endOfNumber = team.number.length
                        spannable.setSpan(android.text.style.ForegroundColorSpan(ContextCompat.getColor(this@HomeActivity, R.color.cyber_blue)), 0, endOfNumber, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        spannable.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, endOfNumber, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

                        binding.teamName.text = spannable
                        binding.programName.text = getString(R.string.program_format, team.program.code, team.program.name)
                        binding.organization.text = team.organization
                        binding.location.text = getString(R.string.location_format, team.location.city, team.location.region, team.location.country)

                        binding.favoriteButton.setOnClickListener {
                            showFavoritesDialog(team.id, team.number, team.team_name)
                        }

                        setButtonsEnabled(true)
                        fetchDashboardData(team.id)
                    } else { navigateToMain() }
                } else { navigateToMain() }
            } catch (e: Exception) {
                Log.e("VEX_DEBUG", "Connection failure", e)
                navigateToMain()
            }
        }
    }

    private fun showFavoritesDialog(currentId: Int, currentNumber: String, currentName: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_favorites, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.favoritesRecyclerView)
        val filterInput = dialogView.findViewById<TextInputEditText>(R.id.filterEditText)
        val addButton = dialogView.findViewById<Button>(R.id.addCurrentTeamButton)
        val clearAllBtn = dialogView.findViewById<Button>(R.id.clearAllButton)

        val dialog = MaterialAlertDialogBuilder(this).setView(dialogView).create()

        val masterFavorites = sharedPref.getStringSet("favorite_teams", emptySet())
            ?.toMutableList()
            ?.apply { sortBy { it.split(":").getOrNull(1) ?: "" } } ?: mutableListOf()

        val displayedList = masterFavorites.toMutableList()

        lateinit var adapter: FavoritesAdapter

        fun checkEmpty() {
            val isEmpty = displayedList.isEmpty()
            recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }

        adapter = FavoritesAdapter(displayedList, currentId) { action, entry, position ->
            if (action == "SELECT") {
                val parts = entry.split(":")
                switchToTeam(parts[0].toInt(), parts[1])
                dialog.dismiss()
            } else if (action == "REMOVE") {
                val updatedSet = sharedPref.getStringSet("favorite_teams", emptySet())?.toMutableSet()
                updatedSet?.remove(entry)
                sharedPref.edit { putStringSet("favorite_teams", updatedSet) }

                masterFavorites.remove(entry)
                displayedList.removeAt(position)
                adapter.notifyItemRemoved(position)
                checkEmpty()
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        checkEmpty()

        filterInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s.toString().lowercase().trim()
                val oldSize = displayedList.size

                val filtered = if (query.isEmpty()) masterFavorites else masterFavorites.filter { it.lowercase().contains(query) }
                val sortedNewList = filtered.sortedBy { it.split(":").getOrNull(1) ?: "" }

                displayedList.clear()
                displayedList.addAll(sortedNewList)

                if (oldSize > displayedList.size) {
                    adapter.notifyItemRangeRemoved(displayedList.size, oldSize - displayedList.size)
                }
                adapter.notifyDataSetChanged()
                checkEmpty()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val currentEntry = "$currentId:$currentNumber:$currentName"
        addButton.visibility = if (masterFavorites.contains(currentEntry)) View.GONE else View.VISIBLE
        addButton.setOnClickListener {
            val updatedSet = sharedPref.getStringSet("favorite_teams", emptySet())?.toMutableSet() ?: mutableSetOf()
            updatedSet.add(currentEntry)
            sharedPref.edit { putStringSet("favorite_teams", updatedSet) }

            masterFavorites.clear()
            masterFavorites.addAll(updatedSet)
            masterFavorites.sortBy { it.split(":").getOrNull(1) ?: "" } // Sort by team number

            displayedList.clear()
            displayedList.addAll(masterFavorites)
            adapter.notifyDataSetChanged()

            addButton.visibility = View.GONE
            checkEmpty()
            showSnackbar("Added $currentNumber")
        }

        clearAllBtn.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Clear All?")
                .setMessage("Remove all favorite teams?")
                .setPositiveButton("Clear") { _, _ ->
                    sharedPref.edit { remove("favorite_teams") }
                    dialog.dismiss()
                    showSnackbar("Favorites cleared")
                }
                .setNegativeButton("Cancel", null).show()
        }

        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val window = dialog.window
        window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.95).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun switchToTeam(newId: Int, newNumber: String) {
        sharedPref.edit {
            putInt("team_id", newId)
            putString("team_number", newNumber)
        }
        this.teamId = newId
        fetchTeamData(newNumber, newId)
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint("#1A1A1A".toColorInt())
            .setTextColor(Color.WHITE)
            .show()
    }

    private fun fetchDashboardData(teamId: Int) {
        lifecycleScope.launch {
            try {
                val seasonsResp = RetrofitClient.service.getSeasons(active = true)
                val seasonIds = seasonsResp.body()?.data?.map { it.id } ?: emptyList()
                if (seasonIds.isNotEmpty()) {
                    val skillsResp = RetrofitClient.service.getTeamSkills(teamId, seasonIds)
                    if (skillsResp.isSuccessful) {
                        val skillsData = skillsResp.body()?.data ?: emptyList()
                        val bestRun = skillsData.groupBy { it.event.id }.map { entry ->
                            val driver = entry.value.filter { it.type.contains("driver", true) }.maxOfOrNull { it.score } ?: 0
                            val prog = entry.value.filter { it.type.contains("prog", true) }.maxOfOrNull { it.score } ?: 0
                            val rank = entry.value.maxByOrNull { it.score }?.rank ?: 0
                            BestSkillsRun(driver, prog, driver + prog, rank)
                        }.maxByOrNull { it.total }
                        if (bestRun != null && bestRun.total > 0) updateSkillsUI(bestRun.driver, bestRun.programming, bestRun.total, bestRun.rank)
                        else updateSkillsUI(null, null, null, null)
                    }
                    val awardsResp = RetrofitClient.service.getTeamAwards(teamId, seasonIds)
                    if (awardsResp.isSuccessful) updateAwardsUI(awardsResp.body()?.data ?: emptyList())
                }
                val eventResponse = RetrofitClient.service.getCompEvents(teamId)
                if (eventResponse.isSuccessful) {
                    val events = eventResponse.body()?.data ?: emptyList()
                    val today = LocalDate.now()
                    val formatter = DateTimeFormatter.ISO_DATE_TIME
                    val nextEvent = events.filter { !it.start.isNullOrEmpty() }.mapNotNull { try { Pair(it, LocalDate.parse(it.start, formatter)) } catch (_: Exception) { null } }.filter { it.second.isAfter(today.minusDays(1)) }.minByOrNull { it.second }
                    if (nextEvent != null) {
                        val daysBetween = ChronoUnit.DAYS.between(today, nextEvent.second)
                        val countdownText = when (daysBetween) { 0L -> "Today" 1L -> "Tomorrow" else -> "In $daysBetween days" }
                        binding.nextEventName.text = nextEvent.first.name
                        val spannableCountdown = android.text.SpannableString(countdownText)
                        spannableCountdown.setSpan(android.text.style.ForegroundColorSpan(ContextCompat.getColor(this@HomeActivity, R.color.accent_gold)), 0, countdownText.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        binding.competitionsSubtitle.text = spannableCountdown
                    }
                }
            } catch (e: Exception) { Log.e("VEX_DEBUG", "Dashboard error", e) }
        }
    }

    private fun updateSkillsUI(driver: Int?, prog: Int?, total: Int?, rank: Int?) {
        if (total != null && total > 0) {
            binding.skillsSeasonBest.text = getString(R.string.skills_season_best_format, total)
            binding.skillsSeasonBest.setTextColor(ContextCompat.getColor(this, R.color.accent_gold))
            binding.skillsBreakdown.visibility = View.VISIBLE
            binding.skillsBreakdown.text = getString(R.string.skills_breakdown_format, driver ?: 0, prog ?: 0)
            if (rank != null && rank > 0) {
                binding.skillsRank.visibility = View.VISIBLE
                binding.skillsRank.text = getString(R.string.rank_format, rank)
            } else { binding.skillsRank.visibility = View.GONE }
        } else {
            binding.skillsSeasonBest.text = getString(R.string.no_skills_runs)
            binding.skillsSeasonBest.setTextColor(ContextCompat.getColor(this, R.color.text_medium_emphasis))
            binding.skillsBreakdown.visibility = View.GONE
            binding.skillsRank.visibility = View.GONE
        }
    }

    private fun updateAwardsUI(awardsList: List<AwardData>) {
        binding.awardsSubtitle.text = getString(R.string.season_total_format, awardsList.size)
        if (awardsList.isNotEmpty()) {
            val awardsSummary = awardsList.groupBy { it.title }.map { entry ->
                val cleanedTitle = entry.key.substringBefore(" (").trim()
                getString(R.string.award_category_format, entry.value.size, cleanedTitle)
            }.joinToString("\n")
            binding.awardsFullList.text = awardsSummary
            binding.awardsFullList.setTextColor(ContextCompat.getColor(this, R.color.cyber_blue))
        } else { binding.awardsFullList.text = getString(R.string.no_awards_earned_yet) }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        val alphaValue = if (enabled) 1.0f else 0.5f
        binding.skillsButton.apply { isEnabled = enabled; alpha = alphaValue }
        binding.competitionsButton.apply { isEnabled = enabled; alpha = alphaValue }
        binding.awardsButton.apply { isEnabled = enabled; alpha = alphaValue }
    }

    private fun launchSection(destination: Class<*>) {
        teamId?.let { id ->
            val intent = Intent(this, destination)
            intent.putExtra("TEAM_ID", id)
            startActivity(intent)
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) showThemedUpdateSnackbar()
            else if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && info.isUpdateTypeAllowed(updateType)) {
                appUpdateManager.startUpdateFlow(info, this, AppUpdateOptions.defaultOptions(updateType))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }

    private fun showThemedUpdateSnackbar() {
        val snackbar = Snackbar.make(binding.root, "Update ready to install.", Snackbar.LENGTH_INDEFINITE)
        snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.cyber_blue))
        snackbar.setBackgroundTint("#1A1A1A".toColorInt())
        snackbar.setTextColor(Color.WHITE)
        snackbar.setAction("RESTART") { appUpdateManager.completeUpdate() }
        snackbar.show()
    }
}

class FavoritesAdapter(
    private val favorites: MutableList<String>,
    private val currentActiveId: Int,
    private val onAction: (String, String, Int) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val teamInfo: TextView = view.findViewById(R.id.teamInfoText)
        val removeBtn: ImageButton = view.findViewById(R.id.removeButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_favorite_team, parent, false)

        view.layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = favorites[position]
        val parts = entry.split(":")
        val id = parts.getOrNull(0) ?: ""
        val number = parts.getOrNull(1) ?: ""
        val name = parts.getOrNull(2) ?: ""

        holder.teamInfo.text = if (name.isNotEmpty()) "$number - $name" else number
        holder.teamInfo.setTextColor(if (id == currentActiveId.toString()) "#FFD54F".toColorInt() else Color.WHITE)

        holder.itemView.setOnClickListener { onAction("SELECT", entry, holder.adapterPosition) }
        holder.removeBtn.setOnClickListener { onAction("REMOVE", entry, holder.adapterPosition) }
    }

    override fun getItemCount() = favorites.size
}

data class BestSkillsRun(val driver: Int, val programming: Int, val total: Int, val rank: Int)