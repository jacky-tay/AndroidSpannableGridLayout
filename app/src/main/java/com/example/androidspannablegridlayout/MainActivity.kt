package com.example.androidspannablegridlayout

import android.os.Bundle
import android.util.Log.i
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.widget.GridLayout
import android.widget.ScrollView
import android.widget.Space
import androidx.annotation.DimenRes
import androidx.appcompat.app.AppCompatActivity
import com.example.androidspannablegridlayout.gridlayout.GridLayoutDefinition
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

class MainActivity : AppCompatActivity(), ViewTreeObserver.OnGlobalLayoutListener,
    ViewTreeObserver.OnScrollChangedListener {

    lateinit var gridLayout: GridLayout
    lateinit var scrollView: ScrollView
    private var columnWidth = 0
    private var visibleRowCount = 0
    private val definitions = prepareItems()
    private val items = (65..90).map { it.toChar().toString() }
    private var cachedCell = SparseArray<ViewHolder>()
    private var reusableIds = mutableListOf<Int>()

    @DimenRes
    var margin = R.dimen.default_gap
    private val gap: Int
        get() = resources.getDimensionPixelSize(margin)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        scrollView = findViewById(R.id.scroll_view)
        scrollView.viewTreeObserver.addOnScrollChangedListener(this)

        gridLayout = findViewById(R.id.grid_layout)
        gridLayout.columnCount = 6
        gridLayout.viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    private fun prepareScrollableContent() {
        val totalRow = definitions.fold(0) { m, i -> max(m, i.maxRow) }
        val params = GridLayout.LayoutParams(
            GridLayout.spec(0, totalRow),
            GridLayout.spec(0, gridLayout.columnCount)
        ).apply {
            width = gridLayout.width
            height = totalRow * columnWidth
        }

        gridLayout.addView(View(applicationContext), params)
    }

    private fun getView() =
        (if (reusableIds.isNotEmpty())
            (cachedCell[reusableIds.removeAt(0)])
        else null) ?: ViewHolder(LayoutInflater.from(applicationContext).inflate(R.layout.list_item_view_holder, null))


    private fun prepareViews() {

        val zero = resources.getDimensionPixelSize(R.dimen.zero)
        gridLayout.setPadding(gap, gap, zero, zero)
        visibleRowCount = ceil(scrollView.height.toDouble() / columnWidth.toDouble()).toInt()
        prepareScrollableContent()

        for (i in 0.until(items.count())) {
            val def = definitions[i]
            // add placeholder
            gridLayout.addView(Space(applicationContext), GridLayout.LayoutParams(
                GridLayout.spec(def.rowStart, def.rowSpan),
                GridLayout.spec(def.colStart, def.colSpan)
            ).apply {
                width = (columnWidth * def.colSpan)
                height = (columnWidth * def.rowSpan)
            })

            if ((def.rowStart * columnWidth) < scrollView.height + columnWidth) {
                val view = getView()
                view.bind(items[i], i)
                layout(view, i)
            }
        }
    }

    private fun layout(holder: ViewHolder, position: Int) {
        val view = holder.itemView
        val def = definitions[position]
        view.tag = holder
        view.id = position
        val params = GridLayout.LayoutParams(
            GridLayout.spec(def.rowStart, def.rowSpan),
            GridLayout.spec(def.colStart, def.colSpan)
        ).apply {
            width = (columnWidth * def.colSpan) - gap
            height = (columnWidth * def.rowSpan) - gap
        }

        if ((def.colStart + def.colSpan) < gridLayout.columnCount)
            params.rightMargin = gap
        params.bottomMargin = gap
        gridLayout.addView(view, params)
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
        columnWidth = (gridLayout.width - resources.getDimensionPixelSize(R.dimen.default_gap)) / gridLayout.columnCount
        prepareViews()
        gridLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
    }
    // endregion

    // region ViewTreeObserver.OnScrollChangedListener
    override fun onScrollChanged() {
        val offsetY = scrollView.scrollY
        val minVisibleRow = max(0, floor((offsetY - gap).toDouble() / columnWidth.toDouble()).toInt())
        val maxVisibleRow = minVisibleRow + visibleRowCount

        val visibleIds = definitions.filter { it.maxRow >= minVisibleRow && it.rowStart <= maxVisibleRow }.map { it.id }
        reusableIds = definitions.map { it.id }.minus(visibleIds).toMutableList()

        definitions.filter { reusableIds.contains(it.id) }.forEach {
            gridLayout.findViewById<View>(it.id)?.let { view ->
                gridLayout.removeView(view)
            }
        }
        definitions.filter { visibleIds.contains(it.id) }.forEach {
            if (gridLayout.findViewById<View>(it.id) == null) {
                val view = getView()
                view.bind(items[it.id], it.id)
                layout(view, it.id)
            }
        }
    }
    // endregion
}
