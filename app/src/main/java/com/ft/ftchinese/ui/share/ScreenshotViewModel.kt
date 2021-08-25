package com.ft.ftchinese.ui.share

import android.app.Application
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.database.ReadArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "ScreenShot"

// TODO: can we merge this into SocialShareViewModel?
class ScreenshotViewModel(application: Application): AndroidViewModel(application) {

    val progressLiveData = MutableLiveData(false)

    // After a row is created in db for the
    // image saving location
    val imageRowCreated: MutableLiveData<ArticleScreenshot> by lazy {
        MutableLiveData<ArticleScreenshot>()
    }

    // After used clicked an app to share.
    val shareSelected: MutableLiveData<SocialAppId> by lazy {
        MutableLiveData<SocialAppId>()
    }

    /**
     * Insert a row for the image-to-be-created in MediaStore
     * The child fragment only need to
     * observe the uri of the image and save screenshot to it.
     * @param article Used to build the ContentValue of this record.
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

            Log.i(TAG, "Screenshot will be saved to $imageUri")

            imageRowCreated.value = ArticleScreenshot(
                imageUri = imageUri,
                content = article
            )
            progressLiveData.value = false
        }
    }

    // Share the screenshot to the specified app.
    fun shareTo(app: SocialApp) {
        shareSelected.value = app.id
    }
}
