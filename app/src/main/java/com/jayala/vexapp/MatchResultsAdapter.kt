package com.jayala.vexapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class MatchResultsAdapter(
    private val items: List<MatchResultItem>,
    private val currentTeamId: Int
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_HEADER = 0
    private val TYPE_MATCH = 1

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val headerText: TextView = view.findViewById(R.id.headerTitle)
    }

    class MatchViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.matchName)
        val redTeams: TextView = view.findViewById(R.id.redTeams)
        val redScore: TextView = view.findViewById(R.id.redScore)
        val blueTeams: TextView = view.findViewById(R.id.blueTeams)
        val blueScore: TextView = view.findViewById(R.id.blueScore)
        val cardView: MaterialCardView = view as MaterialCardView
    }

    override fun getItemViewType(position: Int) =
        if (items[position] is MatchResultItem.Header) TYPE_HEADER else TYPE_MATCH

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_season_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_match_result, parent, false)
            MatchViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]

        if (holder is HeaderViewHolder && item is MatchResultItem.Header) {
            holder.headerText.text = item.divisionName
        } else if (holder is MatchViewHolder && item is MatchResultItem.Match) {
            val match = item.data
            holder.name.text = match.name

            val blue = match.alliances.find { it.color.equals("blue", ignoreCase = true) }
            val blueScore = blue?.score ?: 0
            holder.blueScore.text = blueScore.toString()
            holder.blueTeams.text = blue?.teams?.joinToString("\n") { it.team.name ?: "" }

            val red = match.alliances.find { it.color.equals("red", ignoreCase = true) }
            val redScore = red?.score ?: 0
            holder.redScore.text = redScore.toString()
            holder.redTeams.text = red?.teams?.joinToString("\n") { it.team.name ?: "" }

            val isMyMatch = match.alliances.any { alliance ->
                alliance.teams.any { it.team.id == currentTeamId }
            }

            if (isMyMatch) {
                holder.cardView.strokeColor = ContextCompat.getColor(holder.itemView.context, R.color.cyber_blue)
                holder.cardView.strokeWidth = 3
            } else {
                holder.cardView.strokeColor = ContextCompat.getColor(holder.itemView.context, R.color.divider_dark)
                holder.cardView.strokeWidth = 1
            }

            val goldColor = ContextCompat.getColor(holder.itemView.context, R.color.accent_gold)
            val whiteColor = Color.WHITE

            when {
                blueScore > redScore -> {
                    holder.blueScore.setTextColor(goldColor)
                    holder.redScore.setTextColor(whiteColor)
                }
                redScore > blueScore -> {
                    holder.redScore.setTextColor(goldColor)
                    holder.blueScore.setTextColor(whiteColor)
                }
                else -> {
                    holder.blueScore.setTextColor(whiteColor)
                    holder.redScore.setTextColor(whiteColor)
                }
            }
        }
    }

    override fun getItemCount() = items.size
}