package com.jayala.vexapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jayala.vexapp.databinding.ItemAwardRowBinding

class AwardsAdapter(private val awards: List<AwardUiModel>) :
    RecyclerView.Adapter<AwardsAdapter.AwardViewHolder>() {

    class AwardViewHolder(val binding: ItemAwardRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AwardViewHolder {
        val binding = ItemAwardRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AwardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AwardViewHolder, position: Int) {
        val award = awards[position]
        holder.binding.awardTitleText.text = award.title
        holder.binding.awardEventText.text = award.eventName
        holder.binding.awardDateText.text = award.displayDate
    }

    override fun getItemCount(): Int = awards.size
}