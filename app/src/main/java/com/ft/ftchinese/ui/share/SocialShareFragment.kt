package com.ft.ftchinese.ui.share

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.ft.ftchinese.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Popup for share menu.
 */
class SocialShareFragment(
    private val onClickIcon: (SocialApp) -> Unit
) : BottomSheetDialogFragment() {

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

        return ComposeView(requireContext()).apply {
            setContent {
                SocialShareList(
                    apps = socialApps,
                    onShareTo = { app ->
                        onClickIcon(app)
                        dismiss()
                    }
                )
            }
        }
    }
}



