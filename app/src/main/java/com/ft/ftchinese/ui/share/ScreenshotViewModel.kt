package com.ft.ftchinese.ui.share

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ft.ftchinese.database.ReadArticle
import com.ft.ftchinese.ui.util.ImageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "ScreenShotViewModel"

class ScreenshotViewModel(application: Application): AndroidViewModel(application) {

    val progressLiveData = MutableLiveData(false)

    // After a row is created in db for the
    // image saving location
    val imageRowCreated: MutableLiveData<ScreenshotMeta> by lazy {
        MutableLiveData<ScreenshotMeta>()
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
                val filePath = ImageUtil.getFilePath()

                getApplication<Application>()
                    .contentResolver
                    .insert(
                        filePath,
                        ShareUtils.screenshotDetails(article)
                    )
            } ?: return@launch

            Log.i(TAG, "Screenshot will be saved to $imageUri")

            imageRowCreated.value = ScreenshotMeta(
                imageUri = imageUri,
                title = article.title,
                description = article.standfirst,
            )
            progressLiveData.value = false
        }
    }

}
