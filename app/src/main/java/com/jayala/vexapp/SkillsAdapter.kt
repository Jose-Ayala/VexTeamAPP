package com.jayala.vexapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jayala.vexapp.databinding.ItemSkillRowBinding

class SkillsAdapter(private val items: List<SkillsListItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_HEADER = 0
    private val TYPE_SKILL = 1

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is SkillsListItem.Header -> TYPE_HEADER
            is SkillsListItem.Skill -> TYPE_SKILL
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            val view = inflater.inflate(R.layout.item_season_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val binding = ItemSkillRowBinding.inflate(inflater, parent, false)
            SkillsViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is SkillsListItem.Header -> {
                (holder as HeaderViewHolder).bind(item.seasonName)
            }
            is SkillsListItem.Skill -> {
                val skill = item.model
                val binding = (holder as SkillsViewHolder).binding
                binding.apply {
                    rowEventName.text = skill.eventName
                    rowSeasonName.text = skill.seasonName
                    rowScoreBreakdown.text = "Driver: ${skill.driverScore} | Prog: ${skill.programmingScore}"
                    rowTotalScore.text = "${skill.totalScore}"
                    rowRank.text = if (skill.rank > 0) "Rank: ${skill.rank}" else "Rank: N/A"
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(title: String) {
            (itemView as TextView).text = title
        }
    }

    class SkillsViewHolder(val binding: ItemSkillRowBinding) : RecyclerView.ViewHolder(binding.root)
}