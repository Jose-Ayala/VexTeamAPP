package com.jayala.vexapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jayala.vexapp.databinding.ItemAwardRowBinding

class AwardsAdapter(private val items: List<AwardsListItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_HEADER = 0
    private val TYPE_AWARD = 1

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is AwardsListItem.Header -> TYPE_HEADER
            is AwardsListItem.Award -> TYPE_AWARD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            // Reusing the same header layout used in SkillsActivity
            val view = inflater.inflate(R.layout.item_season_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val binding = ItemAwardRowBinding.inflate(inflater, parent, false)
            AwardViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is AwardsListItem.Header -> {
                (holder as HeaderViewHolder).bind(item.seasonName)
            }
            is AwardsListItem.Award -> {
                val award = item.model
                val binding = (holder as AwardViewHolder).binding
                binding.apply {
                    awardTitleText.text = award.title
                    awardEventText.text = award.eventName
                    awardDateText.text = award.displayDate
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(title: String) {
            // Ensures the item_season_header TextView displays the season name
            (itemView as TextView).text = title
        }
    }

    class AwardViewHolder(val binding: ItemAwardRowBinding) : RecyclerView.ViewHolder(binding.root)
}