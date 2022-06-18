package com.ft.ftchinese.ui.myft

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.model.content.ChannelSource
import com.ft.ftchinese.ui.article.ChannelActivity
import com.ft.ftchinese.ui.theme.Dimens

@Deprecated("")
class FollowingFragment : Fragment() {

    private lateinit var followViewModel: FollowingViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        followViewModel = ViewModelProvider(this)[FollowingViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                FollowScreen(followViewModel = followViewModel)
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = FollowingFragment()
    }
}

@Composable
private fun FollowScreen(
    followViewModel: FollowingViewModel
) {
    val context = LocalContext.current
    val tags by followViewModel.tagsLiveData.observeAsState(listOf())

    LaunchedEffect(key1 = Unit) {
        followViewModel.load()
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(Dimens.dp8),
        verticalArrangement = Arrangement.spacedBy(Dimens.dp8),
        horizontalArrangement = Arrangement.spacedBy(Dimens.dp8),
    ) {
        items(tags) {
            FollowTagCard(
                text = it.tag,
                onClick = {
                    ChannelActivity.start(
                        context,
                        ChannelSource.ofFollowing(it)
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun FollowTagCard(
    text: String,
    onClick: () -> Unit
) {
    Card (
        onClick = onClick
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(Dimens.dp16)
        )
    }
}
