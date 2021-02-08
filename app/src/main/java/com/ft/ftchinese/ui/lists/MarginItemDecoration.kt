package com.ft.ftchinese.ui.lists

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Add margins to recycler view item.
 * See https://medium.com/mobile-app-development-publication/right-way-of-setting-margin-on-recycler-views-cell-319da259b641
 */
class MarginItemDecoration(
    private val marginTop: Int,
    private val marginRight: Int,
    private val marginBottom: Int,
    private val marginLeft: Int,
    ) : RecyclerView.ItemDecoration() {

    constructor(margin: Int) : this(
        marginTop = margin,
        marginRight = margin,
        marginBottom = margin,
        marginLeft = margin
    )

    constructor(topBottom: Int, leftRight: Int): this(
        marginTop = topBottom,
        marginRight = leftRight,
        marginBottom = topBottom,
        marginLeft = leftRight
    )

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        with(outRect) {
            // Handle first row.
            if (parent.getChildAdapterPosition(view) == 0) {
                top = marginTop
            }
            left = marginLeft
            right = marginRight
            bottom = marginBottom
        }
    }
}
