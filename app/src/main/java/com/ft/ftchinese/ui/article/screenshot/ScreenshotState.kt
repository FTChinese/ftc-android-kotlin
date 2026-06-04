package com.ft.ftchinese.ui.article.screenshot

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.ui.article.NavStore
import com.ft.ftchinese.ui.util.toast
import com.ft.ftchinese.ui.util.ImageUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "ScreenshotState"

class ScreenshotState(
    private val scope: CoroutineScope,
    private val context: Context
) {

    var meta by mutableStateOf<ScreenshotMeta?>(null)
        private set

    var bitmap by mutableStateOf<Bitmap?>(null)
        private set

    fun loadImage(id: String) {
        val shotMeta = NavStore.getScreenshot(id)
        if (shotMeta == null) {
            context.toast("Data not found")
            return
        }

        scope.launch {
            val loaded = withContext(Dispatchers.IO) {
                try {
                    ImageUtil.loadAsBitmap(
                        context.contentResolver,
                        shotMeta.imageUri
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load screenshot image ${shotMeta.imageUri}", e)
                    null
                }
            }

            if (loaded == null) {
                context.toast("图片加载失败")
                return@launch
            }

            bitmap = loaded
            meta = shotMeta
        }
    }

}

@Composable
fun rememberScreenshotState(
    scope: CoroutineScope = rememberCoroutineScope(),
    context: Context = LocalContext.current,
) = remember {
    ScreenshotState(
        scope = scope,
        context = context,
    )
}
