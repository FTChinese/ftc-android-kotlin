package com.ft.ftchinese.ui.settings.release

import android.content.Context
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.R
import com.ft.ftchinese.model.AppRelease
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.repository.ReleaseRepo
import com.ft.ftchinese.store.ReleaseStore
import com.ft.ftchinese.ui.base.ConnectionState
import com.ft.ftchinese.ui.base.connectivityState
import com.ft.ftchinese.ui.components.BaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReleaseState(
    scaffoldState: ScaffoldState,
    scope: CoroutineScope,
    connState: State<ConnectionState>,
    context: Context
) : BaseState(scaffoldState, scope, context.resources, connState) {

    private val releaseStore = ReleaseStore(context)

    var newRelease by mutableStateOf<AppRelease?>(null)
        private set

    var downloadStatus by mutableStateOf<DownloadStatus>(DownloadStatus.NotStarted)
        private set

    fun loadRelease() {

        progress.value = true

        scope.launch {
            val cached = loadCachedRelease()
            if (cached != null) {
                if (cached.isNew && cached.isValid) {
                    newRelease = cached
                    progress.value = false
                    return@launch
                } else {
                    releaseStore.clear()
                }
            }

            if (!ensureConnected()) {
                return@launch
            }

            val remote = ReleaseRepo.asyncGetLatest()
            progress.value = false

            when (remote) {
                is FetchResult.LocalizedError -> {
                    showSnackBar(remote.msgId)
                }
                is FetchResult.TextError -> {
                    showSnackBar(remote.text)
                }
                is FetchResult.Success -> {
                    newRelease = remote.data
                    withContext(Dispatchers.IO) {
                        releaseStore.saveLatest(remote.data)
                    }
                }
            }
        }
    }

    fun loadDownloadId(): Long? {
        val pair = releaseStore.loadDownloadId() ?: return null
        if (pair.first < 0) {
            showSnackBar(R.string.download_not_found)
            return null
        }

        return pair.first
    }

    fun downloadStart(id: Long, release: AppRelease) {
        downloadStatus = DownloadStatus.Progress

        scope.launch {
            withContext(Dispatchers.IO) {
                releaseStore.saveDownloadId(id, release)
            }
        }
    }

    fun downloadCompleted() {
        downloadStatus = DownloadStatus.Completed
    }

    private suspend fun loadCachedRelease(): AppRelease? {
        return try {
            withContext(Dispatchers.IO) {
                releaseStore.loadLatest()
            }
        } catch (e: Exception) {
            null
        }
    }
}

@Composable
fun rememberReleaseState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    connState: State<ConnectionState> = connectivityState(),
    scope: CoroutineScope = rememberCoroutineScope(),
    context: Context = LocalContext.current,
) = remember(scaffoldState, connState) {
    ReleaseState(
        scaffoldState = scaffoldState,
        scope = scope,
        connState = connState,
        context = context,
    )
}
