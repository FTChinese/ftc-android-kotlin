package com.ft.ftchinese.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ft.ftchinese.R
import com.ft.ftchinese.model.AppRelease
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.ui.base.ListAdapter
import com.ft.ftchinese.ui.base.ListItem
import com.ft.ftchinese.viewmodel.Result
import com.ft.ftchinese.viewmodel.SettingsViewModel
import com.ft.ftchinese.viewmodel.SettingsViewModelFactory
import kotlinx.android.synthetic.main.fragment_recycler.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

/**
 * Show release log. Used by both [UpdateAppActivity]
 */
class ReleaseLogFragment : Fragment(), AnkoLogger {

    private lateinit var settingsViewModel: SettingsViewModel
    private var listAdapter: ListAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
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
            ViewModelProvider(
                this,
                SettingsViewModelFactory(FileCache(this))
            ).get(SettingsViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        settingsViewModel.releaseResult.observe(viewLifecycleOwner, Observer {

            info("Current release $it")
            onRelease(it)
        })
    }
    
    private fun onRelease(result: Result<AppRelease>) {
        when (result) {
            is Result.LocalizedError -> {
                toast(result.msgId)
            }
            is Result.Error -> {
                result.exception.message?.let { toast(it) }
            }
            is Result.Success -> {
                info("Latest release ${result.data}")
                listAdapter?.setData(buildList(result.data))
            }
        }
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
