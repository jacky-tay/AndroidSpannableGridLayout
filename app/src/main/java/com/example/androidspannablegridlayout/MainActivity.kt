package com.example.androidspannablegridlayout

import android.os.Bundle
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

    private fun getView() =
        (if (reusableIds.isNotEmpty())
            (cachedCell[reusableIds.removeAt(0)])
        else null) ?: ViewHolder(LayoutInflater.from(applicationContext).inflate(R.layout.list_item_view_holder, null))

    private fun prepareViews() {
        val zero = resources.getDimensionPixelSize(R.dimen.zero)
        gridLayout.setPadding(gap, gap, zero, zero)
        visibleRowCount = ceil(scrollView.height.toDouble() / columnWidth.toDouble()).toInt()

        definitions.forEach {
            gridLayout.addView(Space(applicationContext), it.buildLayoutParams(columnWidth))
        } // add placeholders, this is to ensure that when grid layout's view is removed, it won't mess up with content position

        renderVisibleContent()
    }

    private fun layout(holder: ViewHolder, position: Int) {
        val view = holder.itemView
        val def = definitions[position]
        view.tag = position
        val params = def.buildLayoutParams(columnWidth, gap)

        if ((def.colStart + def.colSpan) < gridLayout.columnCount)
            params.rightMargin = gap
        params.bottomMargin = gap
        gridLayout.addView(view, params)
    }

    private fun renderVisibleContent() {
        val offsetY = scrollView.scrollY
        val minVisibleRow = max(0, floor((offsetY - gap).toDouble() / columnWidth.toDouble()).toInt())
        val maxVisibleRow = minVisibleRow + visibleRowCount

        val visibleIds = definitions.filter { it.maxRow >= minVisibleRow && it.rowStart < maxVisibleRow }.map { it.id }
        reusableIds = definitions.map { it.id }.minus(visibleIds).toMutableList()

        definitions.filter { reusableIds.contains(it.id) }.forEach {
            (gridLayout.findViewWithTag<View>(it.id))?.let { view ->
                gridLayout.removeView(view)
            }
        } // remove invisible view from grid layout
        definitions.filter { visibleIds.contains(it.id) && gridLayout.findViewWithTag<View>(it.id) == null }.forEach {
            val view = getView()
            view.bind(items[it.id], it.id)
            layout(view, it.id)
        } // add view if its not visible in grid layout
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
        columnWidth = (gridLayout.width - gap) / gridLayout.columnCount
        prepareViews()
        gridLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
    }
    // endregion

    // region ViewTreeObserver.OnScrollChangedListener
    override fun onScrollChanged() {
        renderVisibleContent()
    }
    // endregion
}
