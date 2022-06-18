package com.ft.ftchinese.ui.myft

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.components.ClickableRow
import com.ft.ftchinese.ui.components.IconRightArrow
import com.ft.ftchinese.ui.main.MyFtActivity
import com.ft.ftchinese.ui.theme.Dimens
import com.ft.ftchinese.ui.theme.OTheme

@Deprecated("")
class ReadArticleFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                OTheme {
                    ReadArticleScreen()
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = ReadArticleFragment()
    }
}

@Composable
private fun ReadArticleScreen() {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        ClickableRow(
            onClick = {
                MyFtActivity.start(context)
            },
            endIcon = {
                IconRightArrow()
            }
        ) {
            Text(text = stringResource(id = R.string.nav_myft))
        }
        Divider(startIndent = Dimens.dp16)
    }
}

