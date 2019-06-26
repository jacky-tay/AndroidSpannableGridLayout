package com.example.androidspannablegridlayout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    lateinit var gridLayoutRecyclerView: GridLayoutRecyclerView<ViewHolder>

    private val items = (65..90).map { it.toChar().toString() }

    inner class Adapter : GridLayoutRecyclerView.Adapter<ViewHolder>() {
        override var definitions: List<GridLayoutRecyclerView.Definition> = prepareItems()

        override fun onBindViewHolder(holder: ViewHolder, id: Int) {
            val index = definitions.indexOfFirst { it.id == id }
            if (index != -1)
                holder.bind(items[index], index)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(LayoutInflater.from(applicationContext).inflate(R.layout.list_item_view_holder, parent, false)!!)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        gridLayoutRecyclerView = findViewById(R.id.grid_layout_recycler_view)
        gridLayoutRecyclerView.columnCount = 6
        gridLayoutRecyclerView.adapter = Adapter()
    }

    private fun prepareItems() = listOf(
        GridLayoutRecyclerView.Definition(0, 2, 2, 0, 0), // A
        GridLayoutRecyclerView.Definition(1, 1, 2, 0, 2), // B
        GridLayoutRecyclerView.Definition(2, 1, 1, 0, 4), // C
        GridLayoutRecyclerView.Definition(3, 1, 1, 0, 5), // D
        GridLayoutRecyclerView.Definition(4, 2, 1, 2, 0), // E
        GridLayoutRecyclerView.Definition(5, 1, 1, 2, 1), // F
        GridLayoutRecyclerView.Definition(6, 2, 2, 1, 2), // G
        GridLayoutRecyclerView.Definition(7, 1, 2, 1, 4), // H
        GridLayoutRecyclerView.Definition(8, 1, 1, 4, 0), // I
        GridLayoutRecyclerView.Definition(9, 3, 3, 3, 1), // J
        GridLayoutRecyclerView.Definition(10, 3, 2, 2, 4), // K
        GridLayoutRecyclerView.Definition(11, 1, 1, 5, 0), // L
        GridLayoutRecyclerView.Definition(12, 1, 4, 6, 0), // M
        GridLayoutRecyclerView.Definition(13, 2, 2, 5, 4), // N
        GridLayoutRecyclerView.Definition(14, 2, 1, 7, 0), // O
        GridLayoutRecyclerView.Definition(15, 1, 2, 7, 1), // P
        GridLayoutRecyclerView.Definition(16, 1, 2, 8, 1), // Q
        GridLayoutRecyclerView.Definition(17, 2, 3, 7, 3), // R
        GridLayoutRecyclerView.Definition(18, 1, 1, 9, 0), // S
        GridLayoutRecyclerView.Definition(19, 2, 1, 9, 1), // T
        GridLayoutRecyclerView.Definition(20, 1, 3, 9, 2), // U
        GridLayoutRecyclerView.Definition(21, 1, 1, 9, 5), // V
        GridLayoutRecyclerView.Definition(22, 1, 4, 10, 2), // W
        GridLayoutRecyclerView.Definition(23, 1, 2, 11, 0), // X
        GridLayoutRecyclerView.Definition(24, 1, 1, 11, 2), // Y
        GridLayoutRecyclerView.Definition(25, 1, 1, 11, 3) // Z
    )
}
