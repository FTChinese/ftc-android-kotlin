package com.ft.ftchinese.ui.share

import android.app.Application
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.database.ReadArticle
import com.ft.ftchinese.model.content.Teaser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScreenshotViewModel(application: Application): AndroidViewModel(application) {

    val progressLiveData = MutableLiveData(false)

    val imageRowCreated: MutableLiveData<ArticleScreenshot> by lazy {
        MutableLiveData<ArticleScreenshot>()
    }

    val shareSelected: MutableLiveData<SocialAppId> by lazy {
        MutableLiveData<SocialAppId>()
    }

    /**
     * Insert a row for the image-to-be-created in MediaStore
     * from [ArticleActivity].
     * The child fragment [WebViewFragment] only need to
     * observe the uri of the image and save screenshot to it.
     * @param teaser Used to build teh ContentValue of this record.
     */
    fun createUri(article: ReadArticle) {
        progressLiveData.value = true
        viewModelScope.launch {
            val imageUri = withContext(Dispatchers.IO) {
                val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(
                        MediaStore.VOLUME_EXTERNAL_PRIMARY
                    )
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }

                getApplication<Application>()
                    .contentResolver
                    .insert(imageCollection, ShareUtils.screenshotDetails(article))
            } ?: return@launch

            imageRowCreated.value = ArticleScreenshot(
                imageUri = imageUri,
                content = article
            )
            progressLiveData.value = false
        }
    }

    fun shareTo(app: SocialApp) {
        shareSelected.value = app.id
    }
}
