package com.ft.ftchinese.ui.lists

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Add margins to grid layout.
 * @param margin
 * @param spanCount - The span count of the grid layout used to determine which one should have a left margin.
 */
@Deprecated("Use compose ui")
class MarginGridDecoration(private val margin: Int, private val spanCount: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        with(outRect) {
            val pos = parent.getChildAdapterPosition(view)

            // Handle the first row.
            if (pos < spanCount) {
                top = margin
            }

            // First column should has a left margin.
            if (pos % spanCount == 0) {
                left = margin
            }

            right = margin
            bottom = margin
        }
    }
}
