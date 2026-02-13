package com.jayala.vexapp

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class MatchRankLeaderboardAdapter(
    private val items: List<MatchRankItem>,
    private val highlightTeamId: Int,
    private val teamNameMap: Map<Int, String>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_HEADER = 0
    private val TYPE_RANK = 1

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is MatchRankItem.Header -> TYPE_HEADER
            is MatchRankItem.Rank -> TYPE_RANK
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_season_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_skill_rank, parent, false)
            RankViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        if (holder is HeaderViewHolder && item is MatchRankItem.Header) {
            holder.headerText.text = item.divisionName
        } else if (holder is RankViewHolder && item is MatchRankItem.Rank) {
            val data = item.data
            val teamId = data.team?.id ?: -1

            val teamNum = data.team?.name ?: "????"

            val teamName = teamNameMap[teamId] ?: ""

            holder.rankText.text = "${data.rank}"

            if (teamName.isNotEmpty() && teamName != teamNum) {
                holder.teamText.text = "$teamNum - $teamName"
            } else {
                holder.teamText.text = teamNum
            }

            val line1 = "W: ${data.wins} | L: ${data.losses} | T: ${data.ties}\n"
            val line2 = "WPs: ${data.wp} | APs: ${data.ap} | SPs: ${data.sp}"

            val spannable = SpannableString(line1 + line2)

            spannable.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(context, R.color.cyber_blue)),
                0,
                line1.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            spannable.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(context, R.color.accent_gold)),
                line1.length,
                (line1 + line2).length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            holder.scoreText.text = spannable

            if (teamId == highlightTeamId) {
                holder.root.strokeColor = ContextCompat.getColor(context, R.color.cyber_blue)
                holder.root.strokeWidth = 6
            } else {
                holder.root.strokeColor = ContextCompat.getColor(context, R.color.divider_dark)
                holder.root.strokeWidth = 2
            }
        }
    }

    override fun getItemCount() = items.size

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val headerText: TextView = view.findViewById(R.id.headerTitle)
    }

    inner class RankViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val root: MaterialCardView = view.findViewById(R.id.skillCard)
        val rankText: TextView = view.findViewById(R.id.rankText)
        val teamText: TextView = view.findViewById(R.id.teamText)
        val scoreText: TextView = view.findViewById(R.id.scoreText)
    }
}