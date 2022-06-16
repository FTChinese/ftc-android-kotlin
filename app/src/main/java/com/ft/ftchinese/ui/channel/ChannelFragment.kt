package com.ft.ftchinese.ui.channel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.ui.theme.OTheme

class ChannelFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val channelSource = arguments
            ?.getParcelable<ChannelSource>(ARG_CHANNEL_SOURCE)

        return ComposeView(requireContext()).apply {

            setContent {
                channelSource?.let {
                    ChannelFragScreen(
                        source = it
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
//        private const val TAG = "ChannelFragment"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        fun newInstance(channel: ChannelSource) = ChannelFragment().apply {
            arguments = bundleOf(ARG_CHANNEL_SOURCE to channel)
        }

    }
}

@Composable
private fun ChannelFragScreen(
    source: ChannelSource,
) {
    val scaffoldState = rememberScaffoldState()

    OTheme {
        Scaffold(
            scaffoldState = scaffoldState
        ) {
            ChannelTabScreen(
                scaffoldState = scaffoldState,
                channelSource = source
            )
        }
    }
}

