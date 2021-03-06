package com.ft.ftchinese.ui.webabout

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityAboutListBinding
import com.ft.ftchinese.ui.lists.SingleLineItemViewHolder

class AboutListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAboutListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_about_list)
        setSupportActionBar(binding.toolbar.toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        binding.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@AboutListActivity).apply {
                orientation = LinearLayoutManager.VERTICAL
            }
            adapter = ListAdapter()
        }
    }

    inner class ListAdapter : RecyclerView.Adapter<SingleLineItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SingleLineItemViewHolder {
            return SingleLineItemViewHolder.create(parent)
        }

        override fun onBindViewHolder(holder: SingleLineItemViewHolder, position: Int) {
            holder.setLeadingIcon(null)
            val item = legalPages[position]
            holder.setText(item.title)

            holder.itemView.setOnClickListener {
                LegalDocActivity.start(this@AboutListActivity, item.url)
            }
        }

        override fun getItemCount() = legalPages.size
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, AboutListActivity::class.java))
        }
    }
}
