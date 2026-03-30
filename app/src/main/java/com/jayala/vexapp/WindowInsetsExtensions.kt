package com.jayala.vexapp

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun View.applyBottomSystemInsetPadding() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { root, insets ->
        val bottomNav = root.findViewById<View?>(R.id.bottomNavBar)
        if (bottomNav != null) {
            val navInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val lp = bottomNav.layoutParams as? ViewGroup.MarginLayoutParams
            if (lp != null) {
                // Cache base margin once, then apply system inset additively without stacking.
                val baseMargin = (bottomNav.tag as? Int) ?: lp.bottomMargin.also { bottomNav.tag = it }
                lp.bottomMargin = baseMargin + navInset
                bottomNav.layoutParams = lp
            }
        }
        insets
    }

    ViewCompat.requestApplyInsets(this)
}
