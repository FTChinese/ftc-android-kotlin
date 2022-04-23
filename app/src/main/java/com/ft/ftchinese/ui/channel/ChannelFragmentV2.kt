package com.ft.ftchinese.ui.channel

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.theme.OTheme
import org.jetbrains.anko.support.v4.toast

private const val TAG = "ChannelFragment"

/**
 * Hosted inside [TabPagerAdapter] or [ChannelActivity]
 * when used to handle pagination.
 */
class ChannelFragmentV2 : ScopedFragment() {

    /**
     * Meta data about current page: the tab's title, where to load data, etc.
     * Passed in when the fragment is created.
     */
    private var channelSource: ChannelSource? = null

    private lateinit var sessionManager: SessionManager

    private lateinit var channelViewModel: ChannelViewModelV2

    override fun onAttach(context: Context) {
        super.onAttach(context)

        sessionManager = SessionManager.getInstance(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get metadata about current tab
        channelSource = arguments
            ?.getParcelable(ARG_CHANNEL_SOURCE)
            ?: return

//        start = Date().time / 1000
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        channelViewModel = ViewModelProvider(this)[ChannelViewModelV2::class.java]

        connectionLiveData.observe(viewLifecycleOwner) {
            channelViewModel.isNetworkAvailable.value = it
        }

        Log.i(TAG, "Channel source $channelSource")
        return ComposeView(requireContext()).apply {
            setContent {
                OTheme {
                    ChannelFragmentScreen(
                        account = sessionManager.loadAccount(),
                        source = channelSource,
                        channelViewModel = channelViewModel,
                        showSnackBar = {
                            toast(it)
                        }
                    )
                }
            }
        }
    }

    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_CHANNEL_SOURCE = "arg_channel_source"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        fun newInstance(channel: ChannelSource) = ChannelFragment().apply {
            arguments = bundleOf(ARG_CHANNEL_SOURCE to channel)
        }

    }
}
