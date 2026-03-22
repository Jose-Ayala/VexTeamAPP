package com.jayala.vexapp

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.roundToInt

fun View.applyBottomSystemInsetPadding() {
    val baseBottomPadding = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        val adjustedInset = (navBarInset * 0.65f).roundToInt()
        view.setPadding(
            view.paddingLeft,
            view.paddingTop,
            view.paddingRight,
            baseBottomPadding + adjustedInset
        )
        insets
    }

    ViewCompat.requestApplyInsets(this)
}
