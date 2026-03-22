package com.jayala.vexapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

data class MatchFilterTeam(
    val id: Int,
    val number: String,
    val name: String
)

class MatchFilterTeamsAdapter(
    private val teams: List<MatchFilterTeam>,
    private var selectedTeamId: Int?,
    private val onTeamSelected: (Int) -> Unit
) : RecyclerView.Adapter<MatchFilterTeamsAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val teamInfo: TextView = view.findViewById(R.id.teamInfoText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_match_filter_team, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val team = teams[position]
        holder.teamInfo.text = if (team.name.isNotBlank()) {
            "${team.number} - ${team.name}"
        } else {
            team.number
        }

        val colorRes = if (team.id == selectedTeamId) R.color.accent_gold else R.color.white
        holder.teamInfo.setTextColor(ContextCompat.getColor(holder.itemView.context, colorRes))

        holder.itemView.setOnClickListener {
            selectedTeamId = team.id
            notifyDataSetChanged()
            onTeamSelected(team.id)
        }
    }

    override fun getItemCount(): Int = teams.size
}

