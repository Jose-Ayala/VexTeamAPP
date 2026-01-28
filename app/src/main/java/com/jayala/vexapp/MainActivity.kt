package com.jayala.vexapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.jayala.vexapp.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt
import com.google.android.play.core.appupdate.AppUpdateOptions

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
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
            else -> { /* No action needed for other statuses */ }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager.registerListener(installStateUpdatedListener)

        val sharedPref = getSharedPreferences("VexPrefs", MODE_PRIVATE)
        if (sharedPref.getInt("team_id", -1) != -1) {
            navigateToHome()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.searchButton.setOnClickListener {
            val teamInput = binding.teamNumberSearch.text.toString().trim().uppercase()
            if (validateFormat(teamInput)) {
                verifyTeamExists(teamInput)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                showThemedUpdateSnackbar()
            }
            else if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && info.isUpdateTypeAllowed(updateType)
            ) {
                appUpdateManager.startUpdateFlow(
                    info,
                    this,
                    AppUpdateOptions.defaultOptions(updateType)
                )
            }
        }
    }

    private fun showThemedUpdateSnackbar() {
        val snackbar = Snackbar.make(
            findViewById(android.R.id.content),
            getString(R.string.update_ready_to_install),
            Snackbar.LENGTH_INDEFINITE
        )

        snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.cyber_blue))
        snackbar.setBackgroundTint("#1A1A1A".toColorInt())
        snackbar.setTextColor(Color.WHITE)

        snackbar.setAction(getString(R.string.restart)) {
            appUpdateManager.completeUpdate()
        }
        snackbar.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }

    private fun validateFormat(input: String): Boolean {
        val vexRegex = Regex("^[0-9A-Z]{2,10}$")
        return when {
            input.isEmpty() -> {
                binding.teamNumberInputLayout.error = getString(R.string.err_enter_team)
                false
            }
            !input.matches(vexRegex) -> {
                binding.teamNumberInputLayout.error = getString(R.string.err_invalid_format)
                false
            }
            else -> {
                binding.teamNumberInputLayout.error = null
                true
            }
        }
    }

    private fun verifyTeamExists(teamNumber: String) {
        lifecycleScope.launch {
            try {
                binding.teamNumberInputLayout.error = null
                binding.teamRecyclerView.visibility = View.GONE
                binding.resultsLabel.visibility = View.GONE

                binding.searchButton.isEnabled = false
                binding.searchButton.text = getString(R.string.verifying_status)

                val response = RetrofitClient.service.getTeamInfo(teamNumber = teamNumber)

                if (response.isSuccessful && response.body() != null) {
                    val teams = response.body()!!.data
                    when {
                        teams.isEmpty() -> binding.teamNumberInputLayout.error = getString(R.string.err_team_not_found)
                        teams.size == 1 -> saveAndNavigate(teams[0].id, teams[0].number)
                        else -> setupTeamTable(teams)
                    }
                } else {
                    binding.teamNumberInputLayout.error = getString(R.string.err_api_failure)
                }
            } catch (e: Exception) {
                // Fix: Log the error to use the 'e' parameter
                Log.e("VEX_DEBUG", "Search failed", e)
                binding.teamNumberInputLayout.error = getString(R.string.err_connection_failure)
            } finally {
                binding.searchButton.isEnabled = true
                binding.searchButton.text = getString(R.string.search_action)
            }
        }
    }

    private fun setupTeamTable(teams: List<TeamData>) {
        binding.resultsLabel.visibility = View.VISIBLE
        binding.teamRecyclerView.visibility = View.VISIBLE
        binding.teamRecyclerView.adapter = TeamAdapter(teams) { selectedTeam ->
            saveAndNavigate(selectedTeam.id, selectedTeam.number)
        }
    }

    private fun saveAndNavigate(teamId: Int, teamNumber: String) {
        val sharedPref = getSharedPreferences("VexPrefs", MODE_PRIVATE)
        sharedPref.edit {
            putInt("team_id", teamId)
            putString("team_number", teamNumber)
        }
        navigateToHome()
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}