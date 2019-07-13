package com.example.androidspannablegridlayout

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.GridLayout
import android.widget.ScrollView
import android.widget.Space
import androidx.annotation.DimenRes
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

class GridLayoutRecyclerView<T> : ScrollView, ViewTreeObserver.OnGlobalLayoutListener,
    ViewTreeObserver.OnScrollChangedListener {
    constructor(context: Context) : super(context)
    constructor(context: Context, attributes: AttributeSet) : super(context, attributes) {
        setAttributes(attributes)
    }

    constructor(context: Context, attributes: AttributeSet, defStyleAttributeSet: Int) : super(
        context, attributes, defStyleAttributeSet
    ) {
        setAttributes(attributes)
    }

    init {
        View.inflate(context, R.layout.grid_layout_recycler_view, this)
        viewTreeObserver.addOnScrollChangedListener(this)
        gridLayout = findViewById(R.id.grid_layout)
        viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    @DimenRes
    var margin = R.dimen.default_gap
    var addSidePadding = true
    var adapter: Adapter<T>? = null
        set(newValue) {
            newValue?.columnCount = gridLayout.columnCount
            newValue?.let { maxRowCount = it.definitions.fold(0) { m, h -> max(m, h.maxRow) } }
            field = newValue
        }
    var columnCount: Int = 1
        set(newValue) {
            gridLayout.columnCount = newValue
            field = newValue
        }
    var rightToLeft = false

    private var maxRowCount: Int? = null
    private var gridLayout: GridLayout
    private var columnWidth = 0
    private var visibleRowCount = 0
    private var cachedCell = SparseArray<T>()
    private var reusableIds = mutableListOf<Int>()
    private var visibleRowRange = 0..0

    private var cellGap: Int? = null
    private val gap: Int
        get() = cellGap ?: resources.getDimensionPixelSize(margin)

    private fun setAttributes(attributes: AttributeSet) {
        val array = context.obtainStyledAttributes(attributes, R.styleable.GridLayoutRecyclerView)
        columnCount = array.getInt(R.styleable.GridLayoutRecyclerView_column_count, 1)
        cellGap = array.getDimensionPixelSize(
            R.styleable.GridLayoutRecyclerView_cell_gap,
            resources.getDimensionPixelSize(R.dimen.zero)
        )
        rightToLeft = array.getBoolean(R.styleable.GridLayoutRecyclerView_right_to_left, rightToLeft)
        addSidePadding = array.getBoolean(R.styleable.GridLayoutRecyclerView_add_side_padding, addSidePadding)
        array.recycle()
    }

    // region Grid Layout Definition
    class Definition(
        val id: Int, val rowSpan: Int, val colSpan: Int,
        var rowStart: Int = -1, var colStart: Int = -1
    ) {
        val maxRow: Int
            get() = rowSpan + rowStart

        val maxCol: Int
            get() = colSpan + colStart

        fun buildLayoutParams(
            columnWidth: Int, rightToLeft: Boolean, columnCount: Int,
            rightGap: Int = 0, bottomGap: Int = 0
        ) = GridLayout.LayoutParams(
            GridLayout.spec(rowStart, rowSpan),
            GridLayout.spec(if (rightToLeft) columnCount - maxCol else colStart, colSpan)
        ).apply {
            width = (columnWidth * colSpan) - rightGap
            height = (columnWidth * rowSpan) - bottomGap
        }
    }
    // endregion

    // region Grid Layout Adapter
    abstract class Adapter<T> {
        internal var columnCount = 6
        internal var definitions: List<Definition> = emptyList()

        open fun getItemViewType(id: Int) = 0

        abstract fun onBindViewHolder(holder: T, id: Int)

        abstract fun onCreateViewHolder(parent: ViewGroup, viewType: Int): T

        private val references = mutableListOf<MutableList<Boolean>>() // true = filled,

        fun prepareLayout(inOrder: List<Definition>) {
            references.clear()
            processDefinition(inOrder)
            definitions = inOrder
        }

        private fun addEmptyReferenceRow(times: Int = 1) =
            repeat(times) { references.add(MutableList(columnCount) { false }) }

        private fun canFitAtRow(row: Int, columnIndex: Int, colSpan: Int) =
            if (columnIndex + colSpan > columnCount || row >= references.count()) false
            else references[row].subList(columnIndex, columnIndex + colSpan).all { !it }

        private fun canFit(fromRow: Int, columnIndex: Int, rowIndex: Int, rowSpan: Int, colSpan: Int): Boolean {
            return when {
                references.count() <= fromRow + rowIndex -> {
                    addEmptyReferenceRow(fromRow + rowSpan - references.count() + 1)
                    true
                }
                rowSpan == rowIndex + 1 -> canFitAtRow(fromRow + rowIndex, columnIndex, colSpan)
                else -> canFitAtRow(fromRow + rowIndex, columnIndex, colSpan) &&
                        canFit(fromRow, columnIndex, rowIndex + 1, rowSpan, colSpan)
            }
        }

        private fun register(row: Int, col: Int, item: Definition): Boolean {
            row.until(row + item.rowSpan).forEach { r ->
                col.until(col + item.colSpan).forEach { c -> references[r][c] = true }
            }
            item.rowStart = row
            item.colStart = col
            return true
        }

        private fun processDefinition(items: List<Definition>) = items.forEach { item ->
            var foundFreeSpace = false
            0.until(references.count()).forEach { r ->
                val index = references[r].indexOfFirst { !it }
                if (!foundFreeSpace && index != -1 && canFit(r, index, 0, item.rowSpan, item.colSpan)) {
                    foundFreeSpace = register(r, index, item)
                }
            }
            if (!foundFreeSpace) {
                val row = references.count()
                addEmptyReferenceRow(item.rowSpan)
                register(row, 0, item)
            }
        }
    }
    // endregion

    // region ViewTreeObserver.OnGlobalLayoutListener
    override fun onGlobalLayout() {
        val sideGap = if (addSidePadding) gap else 0
        columnWidth = (gridLayout.width - sideGap) / gridLayout.columnCount
        prepareViews()
        viewTreeObserver.removeOnGlobalLayoutListener(this)
    }

    private fun prepareViews() {
        val zero = resources.getDimensionPixelSize(R.dimen.zero)
        if (addSidePadding)
            gridLayout.setPadding(gap, gap, zero, zero)
        visibleRowCount = ceil(height.toDouble() / columnWidth.toDouble()).toInt()

        adapter?.definitions?.forEach {
            gridLayout.addView(Space(context), it.buildLayoutParams(columnWidth, rightToLeft, columnCount))
        } // add placeholders, this is to ensure that when grid layout's view is removed, it won't mess up with content position

        renderVisibleContent()
    }
    // endregion

    // region ViewTreeObserver.OnScrollChangedListener
    override fun onScrollChanged() {
        renderVisibleContent()
    }

    private fun renderVisibleContent() = adapter?.apply {
        val offsetY = scrollY
        val topGap = if (addSidePadding) gap else 0
        val minVisibleRow = max(0, floor((offsetY - topGap).toDouble() / columnWidth.toDouble()).toInt())
        val maxVisibleRow = minVisibleRow + visibleRowCount

        if (!visibleRowRange.contains(minVisibleRow) || !visibleRowRange.contains(maxVisibleRow)) {
            val visibleIds = definitions
                .filter { it.maxRow >= minVisibleRow && it.rowStart <= maxVisibleRow }
                .map { it.id }

            reusableIds = definitions.map { it.id }
                .minus(visibleIds)
                .filter { gridLayout.findViewWithTag<View>(it) != null }
                .toMutableList()

            definitions.filter { visibleIds.contains(it.id) && gridLayout.findViewWithTag<View>(it.id) == null }
                .forEach {
                    prepareView(it.id)
                } // add view if its not visible in grid layout
        }
        visibleRowRange = minVisibleRow..maxVisibleRow
    }

    private fun prepareLayout(holder: T, id: Int): GridLayout.LayoutParams? {
        val view = (holder as? RecyclerView.ViewHolder)?.itemView ?: holder as? View ?: return null
        val def = adapter?.definitions?.firstOrNull { it.id == id } ?: return null
        view.tag = def.id
        val rightGap = when {
            !addSidePadding && def.maxCol == gridLayout.columnCount -> 0
            else -> gap
        }
        val bottomGap = when {
            !addSidePadding && def.maxRow == maxRowCount -> 0
            else -> gap
        }

        val params = def.buildLayoutParams(columnWidth, rightToLeft, columnCount, rightGap, bottomGap)

        params.rightMargin = rightGap
        params.bottomMargin = bottomGap
        view.layoutParams = params
        return params
    }

    private fun prepareView(id: Int) = adapter?.apply {
        val viewType = getItemViewType(id)
        var reuseId: Int? = null
        val vh = reusableIds.firstOrNull { getItemViewType(id) == viewType }?.let {
            reuseId = it
            reusableIds.remove(it)
            Log.d("deque", "reuse: $it for $id, reuseSize: ${reusableIds.size}")
            cachedCell[it].apply { cachedCell.remove(it) }
        } ?: onCreateViewHolder(this@GridLayoutRecyclerView, viewType).apply {
            Log.d("deque", "create: $id, reuseSize: ${reusableIds.size}")
        }
        onBindViewHolder(vh, id)
        cachedCell.append(id, vh)
        prepareLayout(vh, id)?.let { params ->
            if (reuseId == null || gridLayout.findViewWithTag<View>(id) == null) ((vh as? RecyclerView.ViewHolder)?.itemView
                ?: vh as? View)?.let {
                gridLayout.addView(it, params)
            } // add view to grid layout if it's newly created
        } // prepareLayout
    }
    // endregion
}
