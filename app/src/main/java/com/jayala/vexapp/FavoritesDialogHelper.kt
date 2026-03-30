package com.jayala.vexapp

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

object FavoritesDialogHelper {

    fun show(
        activity: AppCompatActivity,
        sharedPref: SharedPreferences,
        currentId: Int,
        currentNumber: String,
        currentName: String,
        onTeamSelected: (id: Int, number: String, name: String) -> Unit
    ) {
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_favorites, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.favoritesRecyclerView)
        val filterInput = dialogView.findViewById<TextInputEditText>(R.id.filterEditText)
        val addButton = dialogView.findViewById<Button>(R.id.addCurrentTeamButton)
        val clearAllBtn = dialogView.findViewById<Button>(R.id.clearAllButton)
        val closeButton = dialogView.findViewById<ImageButton>(R.id.closeFavoritesButton)

        val dialog = MaterialAlertDialogBuilder(activity).setView(dialogView).create()
        closeButton.setOnClickListener { dialog.dismiss() }

        val masterFavorites = sharedPref.getStringSet("favorite_teams", emptySet())
            ?.toMutableList()
            ?.apply { sortBy { it.split(":").getOrNull(1) ?: "" } }
            ?: mutableListOf()

        val displayedList = masterFavorites.toMutableList()

        lateinit var adapter: FavoritesAdapter

        fun checkEmpty() {
            recyclerView.visibility = if (displayedList.isEmpty()) View.GONE else View.VISIBLE
        }

        adapter = FavoritesAdapter(displayedList, currentId) { action, entry, position ->
            if (action == "SELECT") {
                val parts = entry.split(":")
                val newId = parts.getOrNull(0)?.toIntOrNull() ?: return@FavoritesAdapter
                val newNumber = parts.getOrNull(1).orEmpty()
                val newName = parts.getOrNull(2).orEmpty()
                onTeamSelected(newId, newNumber, newName)
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

        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
        checkEmpty()

        filterInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s.toString().lowercase().trim()
                val oldSize = displayedList.size

                val filtered = if (query.isEmpty()) {
                    masterFavorites
                } else {
                    masterFavorites.filter { it.lowercase().contains(query) }
                }

                displayedList.clear()
                displayedList.addAll(filtered.sortedBy { it.split(":").getOrNull(1) ?: "" })

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
            masterFavorites.sortBy { it.split(":").getOrNull(1) ?: "" }

            displayedList.clear()
            displayedList.addAll(masterFavorites)
            adapter.notifyDataSetChanged()

            addButton.visibility = View.GONE
            checkEmpty()
            showSnackbar(activity, "Added $currentNumber")
        }

        clearAllBtn.setOnClickListener {
            MaterialAlertDialogBuilder(activity)
                .setTitle("Clear All?")
                .setMessage("Remove all favorite teams?")
                .setPositiveButton("Clear") { _, _ ->
                    sharedPref.edit { remove("favorite_teams") }
                    dialog.dismiss()
                    showSnackbar(activity, "Favorites cleared")
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            (activity.resources.displayMetrics.widthPixels * 0.95).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun showSnackbar(activity: AppCompatActivity, message: String) {
        Snackbar.make(activity.findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint("#1A1A1A".toColorInt())
            .setTextColor(Color.WHITE)
            .show()
    }

    fun applyTeamSelection(
        sharedPref: SharedPreferences,
        currentTeamId: Int,
        newTeamId: Int,
        newTeamNumber: String,
        newTeamName: String
    ): Boolean {
        if (newTeamId == currentTeamId) return false

        sharedPref.edit {
            putInt("team_id", newTeamId)
            putString("team_number", newTeamNumber)
            putString(
                "team_full_name",
                if (newTeamName.isNotEmpty()) "$newTeamNumber - $newTeamName" else newTeamNumber
            )
        }
        return true
    }

    fun navigateHomeWithFullReset(activity: AppCompatActivity) {
        activity.startActivity(Intent(activity, HomeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
        activity.finish()
    }
}
