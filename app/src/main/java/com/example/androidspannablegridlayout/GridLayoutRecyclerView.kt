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
        context,
        attributes,
        defStyleAttributeSet
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
            newValue?.let { maxRowCount = it.definitions.fold(0) { m, h -> max(m, h.maxRow) } }
            field = newValue
        }
    var columnCount: Int = 1
        set(newValue) {
            gridLayout.columnCount = newValue
            field = newValue
        }

    private var maxRowCount: Int? = null
    private var gridLayout: GridLayout
    private var columnWidth = 0
    private var visibleRowCount = 0
    private var cachedCell = SparseArray<T>()
    private var reusableIds = mutableListOf<Int>()

    private var cellGap: Int? = null
    private val gap: Int
        get() = cellGap ?: resources.getDimensionPixelSize(margin)

    private fun setAttributes(attributes: AttributeSet) {
        val array = context.obtainStyledAttributes(attributes, R.styleable.GridLayoutRecyclerView)
        columnCount = array.getInt(R.styleable.GridLayoutRecyclerView_column_count, 1)
        cellGap = array.getDimensionPixelSize(R.styleable.GridLayoutRecyclerView_padding, R.dimen.default_gap)
        addSidePadding = array.getBoolean(R.styleable.GridLayoutRecyclerView_side_padding, addSidePadding)
        array.recycle()
    }

    // region Grid Layout Definition
    class Definition(
        val id: Int,
        private val rowSpan: Int,
        val colSpan: Int,
        var rowStart: Int = 0,
        var colStart: Int = 0
    ) {
        val maxRow: Int
            get() = rowSpan + rowStart

        val maxCol: Int
            get() = colSpan + colStart

        fun buildLayoutParams(columnWidth: Int, rightGap: Int = 0, bottomGap: Int = 0) = GridLayout.LayoutParams(
            GridLayout.spec(rowStart, rowSpan),
            GridLayout.spec(colStart, colSpan)
        ).apply {
            width = (columnWidth * colSpan) - rightGap
            height = (columnWidth * rowSpan) - bottomGap
        }
    }
    // endregion

    // region Grid Layout Adapter
    abstract class Adapter<T> {
        abstract var definitions: List<Definition>
        fun getItemViewType(id: Int) = 0

        abstract fun onBindViewHolder(holder: T, id: Int)

        abstract fun onCreateViewHolder(parent: ViewGroup, viewType: Int): T
    }
    // endregion

    // region ViewTreeObserver.OnGlobalLayoutListener
    override fun onGlobalLayout() {
        val sideGap = if(addSidePadding) gap else 0
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
            gridLayout.addView(Space(context), it.buildLayoutParams(columnWidth))
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

        val visibleIds =
            definitions.filter { it.maxRow >= minVisibleRow && it.rowStart <= maxVisibleRow }.map { it.id }
        reusableIds = definitions.map { it.id }.minus(visibleIds).toMutableList()

        definitions.filter { reusableIds.contains(it.id) }.forEach {
            (gridLayout.findViewWithTag<View>(it.id))?.let { view ->
                gridLayout.removeView(view)
            }
        } // remove invisible view from grid layout

        definitions.filter { visibleIds.contains(it.id) && gridLayout.findViewWithTag<View>(it.id) == null }
            .forEach {
                getView(getItemViewType(it.id))?.let { view ->
                    adapter?.onBindViewHolder(view, it.id)
                    prepareLayout(view, it.id)
                } // view
            } // add view if its not visible in grid layout
    }

    private fun prepareLayout(holder: T, position: Int) {
        val view = (holder as? RecyclerView.ViewHolder)?.itemView ?: holder as? View ?: return
        val def = adapter?.definitions?.get(position) ?: return
        view.tag = def.id
        val rightGap = when {
            !addSidePadding && def.maxCol == gridLayout.columnCount -> 0
            else -> gap
        }
        val bottomGap = when {
            !addSidePadding && def.maxRow == maxRowCount -> 0
            else -> gap
        }

        if (rightGap == 0) {
            Log.i("YOLLO", "Right Gap is zero")
        }

        val params = def.buildLayoutParams(columnWidth, rightGap, bottomGap)

        params.rightMargin = rightGap
        params.bottomMargin = bottomGap

        gridLayout.addView(view, params)
    }

    private fun getView(viewType: Int) =
        (if (reusableIds.isNotEmpty())
            (cachedCell[reusableIds.removeAt(0)])
        else null) ?: adapter?.onCreateViewHolder(this, viewType)
    // endregion
}
