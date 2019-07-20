package com.example.androidspannablegridlayout

import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.list_item_view_holder.view.*

class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    companion object {
        val colors =
            listOf(
                R.color.red,
                R.color.orange,
                R.color.yellow,
                R.color.green,
                R.color.blue,
                R.color.violet,
                R.color.purple
            )
    }

    fun bind(value: String, pos: Int) {
        itemView.text_view.text = value
        itemView.text_view.setBackgroundColor(ContextCompat.getColor(itemView.context, colors[pos]))
        itemView.text_view.setTextColor(
            ContextCompat.getColor(
                itemView.context,
                if (pos < 5) android.R.color.black else android.R.color.white
            )
        )
    }
}
