package com.ft.ftchinese.ui.article.screenshot

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.ui.article.NavStore
import com.ft.ftchinese.ui.util.toast
import com.ft.ftchinese.ui.util.ImageUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            bitmap = withContext(Dispatchers.IO) {
                ImageUtil.loadAsBitmap(
                    context.contentResolver,
                    shotMeta.imageUri
                )
            }

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
