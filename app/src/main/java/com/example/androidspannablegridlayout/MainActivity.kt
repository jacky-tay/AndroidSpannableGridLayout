package com.example.androidspannablegridlayout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import com.example.androidspannablegridlayout.model.BasicExample
import com.example.androidspannablegridlayout.model.Demo
import com.example.androidspannablegridlayout.model.DemoItem
import com.example.androidspannablegridlayout.model.PredefinedExample
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    lateinit var gridLayoutRecyclerView: GridLayoutRecyclerView<ViewHolder, String>

    private lateinit var items: List<GridLayoutRecyclerView.Definition<String>>

    inner class Adapter : GridLayoutRecyclerView.Adapter<ViewHolder, String>() {

        override fun onBindViewHolder(holder: ViewHolder, id: String) {
            val item = items.firstOrNull { it.id == id } as? DemoItem ?: return
            holder.bind(item.id, item.colourId)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(applicationContext).inflate(R.layout.list_item_view_holder, parent, false)!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        gridLayoutRecyclerView = findViewById(R.id.grid_layout_recycler_view)
        gridLayoutRecyclerView.adapter = Adapter()
        load(Demo.Basic)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.let { menuInflater.inflate(R.menu.option, it) }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.basic -> load(Demo.Basic)
            R.id.defined -> load(Demo.Predefined)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun load(type: Demo) {
        val jsonFile = getJsonFromFile(type.file)
        items = when(type) {
            Demo.Basic -> {
                val token = object : TypeToken<List<BasicExample>>() {}.type
                Gson().fromJson<List<BasicExample>>(jsonFile, token)
            }
            Demo.Predefined -> {
                val token = object : TypeToken<List<PredefinedExample>>() {}.type
                Gson().fromJson<List<PredefinedExample>>(jsonFile, token)
            }
        }
        gridLayoutRecyclerView.adapter?.prepareLayout(items)

        supportActionBar?.subtitle = resources.getString(R.string.demo, resources.getString(type.title))
    }

    private fun getJsonFromFile(@RawRes fileResource: Int): String {
        return resources.openRawResource(fileResource).bufferedReader().use { it.readText() }
    }
}
