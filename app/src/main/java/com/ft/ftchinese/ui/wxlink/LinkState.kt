package com.ft.ftchinese.ui.wxlink

import android.content.res.Resources
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.R
import com.ft.ftchinese.model.enums.UnlinkAnchor
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.request.WxLinkParams
import com.ft.ftchinese.model.request.WxUnlinkParams
import com.ft.ftchinese.repository.LinkRepo
import com.ft.ftchinese.ui.components.BaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class LinkState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    resources: Resources
) : BaseState(scaffoldState, scope, resources) {

    fun link(account: Account) {
        val unionId = account.unionId ?: return
        progress.value = true

        scope.launch {

            val doneResult = LinkRepo.asyncLink(
                unionId = unionId,
                params = WxLinkParams(
                    ftcId = account.id,
                ),
            )

            when (doneResult) {
                is FetchResult.LocalizedError -> {
                    progress.value = false
                    showSnackBar(doneResult.msgId)
                }
                is FetchResult.TextError -> {
                    progress.value = false
                    showSnackBar(doneResult.text)
                }
                is FetchResult.Success -> {
                    refresh(account)
                }
            }
        }
    }

    fun unlink(account: Account, anchor: UnlinkAnchor) {
        val unionId = account.unionId
        if (unionId == null) {
            showSnackBar(R.string.unlink_missing_union_id)
            return
        }

        progress.value = true

        val params = WxUnlinkParams(
            ftcId = account.id,
            anchor = anchor,
        )

        scope.launch {
            val doneResult = LinkRepo.asyncUnlink(
                unionId = unionId,
                params = params,
            )

            when (doneResult) {
                is FetchResult.LocalizedError -> {
                    progress.value = false
                    showSnackBar(doneResult.msgId)
                }
                is FetchResult.TextError -> {
                    progress.value = false
                    showSnackBar(doneResult.text)
                }
                is FetchResult.Success -> {
                    refresh(account)
                }
            }
        }
    }
}

@Composable
fun rememberLinkState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    resources: Resources = LocalContext.current.resources
) = remember(scaffoldState, scope, resources) {
    LinkState(
        scaffoldState = scaffoldState,
        scope = scope,
        resources = resources
    )
}
