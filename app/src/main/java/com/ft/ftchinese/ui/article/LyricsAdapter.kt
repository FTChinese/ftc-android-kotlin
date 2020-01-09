package com.ft.ftchinese.ui.article

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R

private const val VIEW_TYPE_HEADING = 0
private const val VIEW_TYPE_TEXT = 1

class LyricsAdapter(private var dataset: List<String>) : RecyclerView.Adapter<LyricsAdapter.ViewHolder>() {

    class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val textView = when (viewType) {
            VIEW_TYPE_HEADING -> LayoutInflater.from(parent.context)
                    .inflate(R.layout.lyrics_heading, parent, false) as TextView

            VIEW_TYPE_TEXT -> LayoutInflater.from(parent.context)
                    .inflate(R.layout.lyrics_text_view, parent, false) as TextView
            else -> LayoutInflater.from(parent.context)
                .inflate(R.layout.lyrics_text_view, parent, false) as TextView
        }

        return ViewHolder(textView)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            VIEW_TYPE_HEADING
        } else {
            VIEW_TYPE_TEXT
        }
    }

    override fun getItemCount() = dataset.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = dataset[position].replace("<p>", "").replace("</p>", "")
    }

    fun setData(data: List<String>) {
        this.dataset = data
    }
}
