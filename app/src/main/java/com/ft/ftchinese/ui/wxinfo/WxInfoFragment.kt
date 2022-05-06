package com.ft.ftchinese.ui.wxinfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.ui.theme.OTheme
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import org.jetbrains.anko.support.v4.toast

class WxInfoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                OTheme {
                    WxInfoActivityScreen(
                        wxApi = WXAPIFactory.createWXAPI(requireContext(), BuildConfig.WX_SUBS_APPID).apply {
                                                                                        registerApp(BuildConfig.WX_SUBS_APPID)
                        },
                        showSnackBar = {
                            toast(it)
                        }
                    )
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = WxInfoFragment()
    }
}
