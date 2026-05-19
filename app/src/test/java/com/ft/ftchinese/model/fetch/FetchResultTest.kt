package com.ft.ftchinese.model.fetch

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class FetchResultTest {

    @Test
    fun networkExceptionsUseGenericLoadingFailure() {
        assertEquals(
            FetchResult.loadingFailed,
            FetchResult.fromException(IOException("timeout"))
        )
    }

    @Test
    fun emptyExceptionMessageDoesNotCreateBlankError() {
        assertEquals(
            FetchResult.unknownError,
            FetchResult.fromException(Exception())
        )
    }
}
