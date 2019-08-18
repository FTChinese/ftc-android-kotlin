package com.ft.ftchinese.ui.settings

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager

import com.ft.ftchinese.R
import com.ft.ftchinese.model.AppRelease
import com.ft.ftchinese.ui.base.ListAdapter
import com.ft.ftchinese.ui.base.ListItem
import kotlinx.android.synthetic.main.fragment_recycler.*

class ReleaseLogFragment : Fragment() {

    private lateinit var settingsViewModel: SettingsViewModel
    private var listAdapter: ListAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_recycler, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listAdapter = ListAdapter(listOf())

        recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = listAdapter
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        settingsViewModel = activity?.run {
            ViewModelProvider(this)
                    .get(SettingsViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        settingsViewModel.releaseResult.observe(this, Observer {

            if (it.success == null) {
                return@Observer
            }

            listAdapter?.setData(buildList(it.success))
        })
    }

    private fun buildList(release: AppRelease): List<ListItem> {

        val contents = release.splitBody().map {
            ListItem(
                    primaryText = it,
                    iconVisibility = View.VISIBLE
            )
        }

        View.VISIBLE
        return mutableListOf(
                ListItem(
                        primaryText = getString(R.string.version_name),
                        secondaryText = release.versionName,
                        iconVisibility = View.INVISIBLE
                ),
                ListItem(
                        primaryText = getString(R.string.released_at),
                        secondaryText = release.creationTime,
                        iconVisibility = View.INVISIBLE
                ),
                ListItem(
                        secondaryText = getString(R.string.pref_release_log),
                        iconVisibility = View.INVISIBLE
                )
        ).plus(contents)
    }

    companion object {

        @JvmStatic
        fun newInstance() = ReleaseLogFragment()
    }
}
