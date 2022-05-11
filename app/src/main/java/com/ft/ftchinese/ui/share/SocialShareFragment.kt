package com.ft.ftchinese.ui.share

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Popup for share menu.
 */
class SocialShareFragment :
        BottomSheetDialogFragment() {

    private lateinit var viewModel: SocialShareViewModel

    private val socialApps = listOf(
        SocialApp(
            name = "好友",
            icon = R.drawable.wechat,
            id = SocialAppId.WECHAT_FRIEND
        ),
        SocialApp(
            name = "朋友圈",
            icon = R.drawable.moments,
            id = SocialAppId.WECHAT_MOMENTS
        ),
        SocialApp(
            name = "打开链接",
            icon = R.drawable.chrome,
            id = SocialAppId.OPEN_IN_BROWSER
        ),
        SocialApp(
            name = "全文截屏",
            icon = R.drawable.screenshot,
            id = SocialAppId.SCREENSHOT,
        ),
        SocialApp(
            name = "更多",
            icon = R.drawable.ic_more_horiz_black_24dp,
            id = SocialAppId.MORE_OPTIONS
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        viewModel = activity?.run {
            ViewModelProvider(this)[SocialShareViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        return ComposeView(requireContext()).apply {
            setContent {
                SocialShareList(
                    apps = socialApps,
                    onShareTo = { app ->
                        viewModel.select(app)
                        dismiss()
                    }
                )
            }
        }
    }
}



