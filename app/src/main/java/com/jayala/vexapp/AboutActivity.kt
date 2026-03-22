package com.jayala.vexapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.net.toUri
import com.jayala.vexapp.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applyBottomSystemInsetPadding()

        // Dynamically get version name from build.gradle
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (_: Exception) {
            "1.0"
        }
        binding.appVersion.text = getString(R.string.version, versionName)

        binding.backButton.setOnClickListener {
            finish()
        }
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

        binding.githubCard.setOnClickListener {
            val url = "https://github.com/Jose-Ayala/VexTeamAPP"
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        }

        binding.contactCard.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = "mailto:jayala4145@gmail.com".toUri()
                putExtra(Intent.EXTRA_SUBJECT, "VEX Team App Support - v$versionName")
            }
            try {
                startActivity(Intent.createChooser(emailIntent, "Send Email Support..."))
            } catch (_: Exception) {
                // Handle case where no email app is installed
            }
        }

        binding.privacyCard.setOnClickListener {
            val url = "https://github.com/Jose-Ayala/VexTeamAPP/blob/main/PRIVACY_POLICY.md"
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        }
    }
}