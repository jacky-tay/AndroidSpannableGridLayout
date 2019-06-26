package com.example.androidspannablegridlayout.gridlayout

import android.widget.GridLayout

class GridLayoutDefinition(
    val id: Int,
    val rowSpan: Int,
    val colSpan: Int,
    var rowStart: Int = 0,
    var colStart: Int = 0
) {
    val maxRow: Int
        get() = rowSpan + rowStart

    fun buildLayoutParams(columnWidth: Int, gap: Int = 0) = GridLayout.LayoutParams(
        GridLayout.spec(rowStart, rowSpan),
        GridLayout.spec(colStart, colSpan)
    ).apply {
        width = (columnWidth * colSpan) - gap
        height = (columnWidth * rowSpan) - gap
    }
}