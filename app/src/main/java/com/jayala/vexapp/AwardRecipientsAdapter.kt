package com.jayala.vexapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jayala.vexapp.databinding.ItemSkillRankBinding

class AwardRecipientsAdapter(
    private val awards: List<CompAwardData>,
    private val myTeamId: Int,
    private val teamNameMap: Map<Int, String>
) : RecyclerView.Adapter<AwardRecipientsAdapter.AwardViewHolder>() {

    class AwardViewHolder(val binding: ItemSkillRankBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AwardViewHolder {
        val binding = ItemSkillRankBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AwardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AwardViewHolder, position: Int) {
        val award = awards[position]
        val context = holder.itemView.context

        holder.binding.rankText.visibility = View.GONE
        holder.binding.teamText.text = award.title.substringBefore("(").trim()

        holder.binding.teamText.setTextColor(androidx.core.content.ContextCompat.getColor(context, android.R.color.white))

        val isMyWin = award.teamWinners?.any { it.team?.id == myTeamId } == true

        if (isMyWin) {
            val gold = androidx.core.content.ContextCompat.getColor(context, R.color.accent_gold)
            val cyberBlue = androidx.core.content.ContextCompat.getColor(context, R.color.cyber_blue)

            holder.binding.scoreText.setTextColor(gold)
            holder.binding.skillCard.setStrokeColor(android.content.res.ColorStateList.valueOf(cyberBlue))
            holder.binding.skillCard.strokeWidth = 4
        } else {
            holder.binding.scoreText.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.cyber_blue))
            holder.binding.skillCard.setStrokeColor(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.divider_dark)))
            holder.binding.skillCard.strokeWidth = 2
        }

        holder.binding.scoreText.text = award.teamWinners?.joinToString("\n") { winner ->
            val teamId = winner.team?.id ?: -1
            val teamNum = winner.team?.name ?: "????"
            val teamName = teamNameMap[teamId] ?: ""

            if (teamName.isNotEmpty() && teamName != teamNum) {
                "$teamNum - $teamName"
            } else {
                teamNum
            }
        } ?: "No winners"
    }

    override fun getItemCount() = awards.size
}