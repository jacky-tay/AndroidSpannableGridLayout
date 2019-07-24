package spannable.grid.layout

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.GridLayout
import android.widget.ScrollView
import android.widget.Space
import androidx.annotation.DimenRes
import androidx.recyclerview.widget.RecyclerView
import java.lang.ref.WeakReference
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

class GridLayoutRecyclerView<T, D> : ScrollView, ViewTreeObserver.OnGlobalLayoutListener,
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
    var adapter: Adapter<T, D>? = null
        set(newValue) {
            newValue?.columnCount = gridLayout.columnCount
            newValue?.weakReference = WeakReference(this)
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
    private var cachedCell = mutableMapOf<D, T>()
    private var reusableIds = mutableListOf<D>()
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
    open class Definition<D>(
        val id: D, val rowSpan: Int, val colSpan: Int,
        var rowStart: Int? = null, var colStart: Int? = null
    ) {
        val maxRow: Int
            get() = rowSpan + (rowStart ?: 0)

        val maxCol: Int
            get() = colSpan + (colStart ?: 0)

        fun defined() = ((rowStart ?: -1) >= 0) && ((colStart ?: -1) >= 0)

        fun buildLayoutParams(
            columnWidth: Int, rightToLeft: Boolean, columnCount: Int,
            rightGap: Int = 0, bottomGap: Int = 0
        ) = GridLayout.LayoutParams(
            GridLayout.spec(rowStart ?: 0, rowSpan),
            GridLayout.spec(if (rightToLeft) columnCount - maxCol else colStart ?: 0, colSpan)
        ).apply {
            width = (columnWidth * colSpan) - rightGap
            height = (columnWidth * rowSpan) - bottomGap
        }
    }
    // endregion

    // region Grid Layout Adapter
    abstract class Adapter<T, D> {
        internal lateinit var weakReference: WeakReference<GridLayoutRecyclerView<T, D>>
        internal var columnCount: Int = 1
        internal var definitions: List<Definition<D>> = emptyList()

        open fun getItemViewType(id: D) = 0

        abstract fun onBindViewHolder(holder: T, id: D)

        abstract fun onCreateViewHolder(parent: ViewGroup, viewType: Int): T

        private val references = mutableListOf<MutableList<Boolean>>() // true = filled,

        fun prepareLayout(inOrder: List<Definition<D>>) {
            references.clear()
            val undefined = preparePredefinedLayout(inOrder)
            columnCount = max(columnCount, undefined.maxBy { it.colSpan }?.colSpan ?: 1)
            weakReference.get()?.columnCount = columnCount
            processDefinition(undefined)
            definitions = inOrder
            weakReference.get()?.reloadIfNeeded()
        }

        private fun preparePredefinedLayout(inOrder: List<Definition<D>>): List<Definition<D>> {
            val defined = inOrder.filter { it.defined() }
            val undefined = inOrder.filter { !it.defined() }.toMutableList()
            columnCount = max(columnCount, (defined.maxBy { it.maxCol }?.maxCol ?: 1))
            val rowCount = defined.maxBy { it.maxRow }?.maxRow ?: 1
            addEmptyReferenceRow(rowCount)
            for (item in defined) {
                if (canFit(item.rowStart, item.colStart, item.rowSpan, item.colSpan)) {
                    register(item.rowStart, item.colStart, item)
                } else {
                    val index = inOrder.indexOf(item)
                    if (index != -1) {
                        var pointer = index
                        while (pointer < inOrder.count() && !inOrder[pointer].defined())
                            pointer += 1
                        if (pointer < inOrder.count()) {
                            val anchorDefinition = inOrder[pointer]
                            val anchorIndex = undefined.indexOf(anchorDefinition)
                            if (anchorIndex != -1)
                                undefined.add(anchorIndex, item)
                        }
                    }
                }
            }
            return undefined
        }

        private fun addEmptyReferenceRow(times: Int = 1) =
            repeat(times) { references.add(MutableList(columnCount) { false }) }

        private fun canFitAtRow(row: Int, columnIndex: Int, colSpan: Int) =
            if (columnIndex + colSpan > columnCount || row >= references.count()) false
            else references[row].subList(columnIndex, columnIndex + colSpan).all { !it }

        private fun canFit(fromRow: Int?, columnIndex: Int?, rowSpan: Int, colSpan: Int, rowIndex: Int = 0): Boolean {
            return when {
                fromRow == null || columnIndex == null -> false
                references.count() <= fromRow + rowIndex -> {
                    addEmptyReferenceRow(fromRow + rowSpan - references.count() + 1)
                    true
                }
                rowSpan == rowIndex + 1 -> canFitAtRow(fromRow + rowIndex, columnIndex, colSpan)
                else -> canFitAtRow(fromRow + rowIndex, columnIndex, colSpan) &&
                        canFit(fromRow, columnIndex, rowSpan, colSpan, rowIndex + 1)
            }
        }

        private fun register(row: Int?, col: Int?, item: Definition<D>): Boolean {
            if (row == null || col == null) return false

            row.until(row + item.rowSpan).forEach { r ->
                col.until(col + item.colSpan).forEach { c -> references[r][c] = true }
            }
            item.rowStart = row
            item.colStart = col
            return true
        }

        private fun processDefinition(items: List<Definition<D>>) = items.forEach { item ->
            var foundFreeSpace = false
            0.until(references.count()).forEach { r ->
                val index = references[r].indexOfFirst { !it }
                if (!foundFreeSpace && index != -1 && canFit(r, index, item.rowSpan, item.colSpan)) {
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

    internal fun reloadIfNeeded() {
        if (gridLayout.childCount > 0) {
            gridLayout.removeAllViews()
            cachedCell.clear()
            reusableIds.clear()
            visibleRowRange = 0..0
            val sideGap = if (addSidePadding) gap else 0
            columnWidth = (gridLayout.width - sideGap) / gridLayout.columnCount
            prepareViews()
        }
    }

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
                .filter { it.maxRow >= minVisibleRow && (it.rowStart ?: -1) <= maxVisibleRow }
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

    private fun prepareLayout(holder: T, id: D): GridLayout.LayoutParams? {
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

    private fun prepareView(id: D) = adapter?.apply {
        val viewType = getItemViewType(id)
        var reuseId: D? = null
        val vh = reusableIds.firstOrNull { getItemViewType(id) == viewType }?.let {
            reuseId = it
            reusableIds.remove(it)
            Log.d("deque", "reuse: $it for $id, reuseSize: ${reusableIds.size}")
            cachedCell[it].apply { cachedCell.remove(it) }
        } ?: onCreateViewHolder(this@GridLayoutRecyclerView, viewType).apply {
            Log.d("deque", "create: $id, reuseSize: ${reusableIds.size}")
        }
        onBindViewHolder(vh, id)
        cachedCell[id] = vh
        prepareLayout(vh, id)?.let { params ->
            if (reuseId == null || gridLayout.findViewWithTag<View>(id) == null) ((vh as? RecyclerView.ViewHolder)?.itemView
                ?: vh as? View)?.let {
                gridLayout.addView(it, params)
            } // add view to grid layout if it's newly created
        } // prepareLayout
    }
    // endregion
}
