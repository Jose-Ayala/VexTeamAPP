package com.jayala.vexapp

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF

// @JvmOverloads creates the (Context), (Context, AttributeSet), etc. constructors for you
class SkillsMarkerView @JvmOverloads constructor(
    context: Context,
    layoutResource: Int,
    private val models: List<SkillsUiModel> = emptyList()
) : MarkerView(context, layoutResource) {

    private val tvEvent: TextView = findViewById(R.id.markerEvent)
    private val tvTotal: TextView = findViewById(R.id.markerTotal)
    private val tvBreakdown: TextView = findViewById(R.id.markerBreakdown)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        // Entries are 1-indexed in our chart logic; adjust for 0-indexed list
        val index = (e?.x?.toInt() ?: 1) - 1

        if (index >= 0 && index < models.size) {
            val model = models[index]
            tvEvent.text = model.eventName

            // Using string resources with placeholders to satisfy the previous warnings
            tvTotal.text = context.getString(R.string.marker_total, model.totalScore)
            tvBreakdown.text = context.getString(
                R.string.marker_breakdown,
                model.driverScore,
                model.programmingScore
            )
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        // Centers the tooltip horizontally and places it above the data point
        return MPPointF((-(width / 2)).toFloat(), (-height).toFloat())
    }
}