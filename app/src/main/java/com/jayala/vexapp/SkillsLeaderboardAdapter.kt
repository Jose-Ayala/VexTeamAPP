package com.jayala.vexapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

data class TeamSkillSummary(
    val teamId: Int,
    val teamNumber: String,
    val driver: Int,
    val programming: Int,
    val total: Int,
    val rank: Int,
    val teamName: String
)

class SkillsLeaderboardAdapter(
    private val items: List<TeamSkillSummary>,
    private val highlightTeamId: Int,
    private val teamNameMap: Map<Int, String>
) : RecyclerView.Adapter<SkillsLeaderboardAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val root: MaterialCardView = view.findViewById(R.id.skillCard)
        val rankText: TextView = view.findViewById(R.id.rankText)
        val teamText: TextView = view.findViewById(R.id.teamText)
        val scoreText: TextView = view.findViewById(R.id.scoreText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_skill_rank, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        holder.rankText.text = "${item.rank}"

        if (item.teamName.isNotEmpty() && item.teamName != item.teamNumber) {
            holder.teamText.text = "${item.teamNumber} - ${item.teamName}"
        } else {
            holder.teamText.text = item.teamNumber
        }

        holder.scoreText.text = "Dr: ${item.driver} | Prog: ${item.programming} | Total: ${item.total}"

        holder.rankText.setTextColor(Color.WHITE)
        holder.teamText.setTextColor(Color.WHITE)

        holder.scoreText.setTextColor(ContextCompat.getColor(context, R.color.accent_gold))

        if (item.teamId == highlightTeamId) {
            val cyberBlue = ContextCompat.getColor(context, R.color.cyber_blue)
            holder.root.strokeColor = cyberBlue
            holder.root.strokeWidth = 6
        } else {
            holder.root.strokeColor = ContextCompat.getColor(context, R.color.divider_dark)
            holder.root.strokeWidth = 2
        }
    }

    override fun getItemCount() = items.size
}