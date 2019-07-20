package spannable.grid.layout.model

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import spannable.grid.layout.GridLayoutRecyclerView
import spannable.grid.layout.R

enum class Demo( @StringRes val title: Int, @RawRes val file: Int) {
    Basic(R.string.basic, R.raw.basic_example),
    Predefined(R.string.defined, R.raw.basic_example_defined)
}

interface DemoItem {
    val id: String
    val colourId: Int
}

class BasicExample(id: String, rowSpan: Int, colSpan: Int, override val colourId: Int): GridLayoutRecyclerView.Definition<String>(id, rowSpan, colSpan), DemoItem

class PredefinedExample(id: String, rowSpan: Int, colSpan: Int, rowStart: Int, colStart: Int, override val colourId: Int): GridLayoutRecyclerView.Definition<String>(id, rowSpan, colSpan, rowStart, colStart), DemoItem
