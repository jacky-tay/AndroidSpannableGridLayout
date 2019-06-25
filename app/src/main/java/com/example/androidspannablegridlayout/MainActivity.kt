package com.example.androidspannablegridlayout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewTreeObserver
import android.widget.GridLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.androidspannablegridlayout.gridlayout.GridLayoutDefinition

class MainActivity : AppCompatActivity(), ViewTreeObserver.OnGlobalLayoutListener {

    lateinit var gridLayout: GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        gridLayout = findViewById(R.id.grid_layout)
        gridLayout.columnCount = 6
        gridLayout.viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    private fun addViews(width: Int) {
        val definitions = prepareItems()
        val items = (65..90).map { it.toChar().toString() }
        val margin = resources.getDimensionPixelSize(R.dimen.default_gap)
        val zero = resources.getDimensionPixelSize(R.dimen.zero)
        gridLayout.setPadding(margin, margin, zero, zero)

        for (i in 0.until(items.count())) {
            val def = definitions[i]
            val view = ViewHolder(LayoutInflater.from(applicationContext).inflate(R.layout.list_item_view_holder, null))
            view.bind(items[i], i)
            val param = GridLayout.LayoutParams(GridLayout.spec(def.rowStart, def.rowSpan), GridLayout.spec(def.colStart, def.colSpan))
            param.width = (width * def.colSpan) - margin
            param.height = (width * def.rowSpan) - margin
            if ((def.colStart + def.colSpan) < gridLayout.columnCount)
                param.rightMargin = margin
            param.bottomMargin = margin
            gridLayout.addView(view.itemView, param)
        }
    }

    private fun prepareItems() = listOf(
        GridLayoutDefinition(0, 2, 2, 0, 0), // A
        GridLayoutDefinition(1, 1, 2, 0, 2), // B
        GridLayoutDefinition(2, 1, 1, 0, 4), // C
        GridLayoutDefinition(3, 1, 1, 0, 5), // D
        GridLayoutDefinition(4, 2, 1, 2, 0), // E
        GridLayoutDefinition(5, 1, 1, 2, 1), // F
        GridLayoutDefinition(6, 2, 2, 1, 2), // G
        GridLayoutDefinition(7, 1, 2, 1, 4), // H
        GridLayoutDefinition(8, 1, 1, 4, 0), // I
        GridLayoutDefinition(9, 3, 3, 3, 1), // J
        GridLayoutDefinition(10, 3, 2, 2, 4), // K
        GridLayoutDefinition(11, 1, 1, 5, 0), // L
        GridLayoutDefinition(12, 1, 4, 6, 0), // M
        GridLayoutDefinition(13, 2, 2, 5, 4), // N
        GridLayoutDefinition(14, 2, 1, 7, 0), // O
        GridLayoutDefinition(15, 1, 2, 7, 1), // P
        GridLayoutDefinition(16, 1, 2, 8, 1), // Q
        GridLayoutDefinition(17, 2, 3, 7, 3), // R
        GridLayoutDefinition(18, 1, 1, 9, 0), // S
        GridLayoutDefinition(19, 2, 1, 9, 1), // T
        GridLayoutDefinition(20, 1, 3, 9, 2), // U
        GridLayoutDefinition(21, 1, 1, 9, 5), // V
        GridLayoutDefinition(22, 1, 4, 10, 2), // W
        GridLayoutDefinition(23, 1, 2, 11, 0), // X
        GridLayoutDefinition(24, 1, 1, 11, 2), // Y
        GridLayoutDefinition(25, 1, 1, 11, 3) // Z
    )

    // region ViewTreeObserver.OnGlobalLayoutListener
    override fun onGlobalLayout() {
        val colWidth = (gridLayout.width - resources.getDimensionPixelSize(R.dimen.default_gap)) / gridLayout.columnCount
        addViews(colWidth)
        gridLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
    }
    // endregion
}
