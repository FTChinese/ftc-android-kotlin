package com.ft.ftchinese.ui.article.screenshot

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.database.ArticleDb
import com.ft.ftchinese.ui.util.ImageUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScreenshotState(
    private val scope: CoroutineScope,
    private val context: Context
) {
    private val db = ArticleDb.getInstance(context)

    var meta by mutableStateOf<ScreenshotMeta?>(null)
        private set

    var bitmap by mutableStateOf<Bitmap?>(null)
        private set

    fun loadImage(params: ScreenshotParams) {
        val uri = Uri.parse(params.imageUrl) ?: return

        scope.launch {
            bitmap = withContext(Dispatchers.IO) {
                ImageUtil.loadAsBitmap(
                    context.contentResolver,
                    uri
                )
            }

            val article = withContext(Dispatchers.IO) {
                db.readDao().getOne(
                    id = params.articleId,
                    type = params.articleType
                )
            }

            meta = ScreenshotMeta(
                imageUri = uri,
                title = article?.title ?: "",
                description = article?.standfirst ?: "",
            )
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
