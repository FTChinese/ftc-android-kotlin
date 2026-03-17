package com.ft.ftchinese.model.fetch

import com.ft.ftchinese.repository.Endpoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FetchTest {

    @Test
    fun publicApiRequestsKeepUsingAppToken() {
        val auth = Fetch.resolveApiAuthorization(
            sessionToken = "session-token",
            userId = null,
            unionId = null,
        )

        assertFalse(auth.usedSessionToken)
        assertEquals("Bearer ${Endpoint.accessToken}", auth.headerValue)
    }

    @Test
    fun userScopedRequestsUseSessionTokenWhenAvailable() {
        val auth = Fetch.resolveApiAuthorization(
            sessionToken = "session-token",
            userId = "ftc-user-id",
            unionId = null,
        )

        assertTrue(auth.usedSessionToken)
        assertEquals("Bearer session-token", auth.headerValue)
    }

    @Test
    fun unionScopedRequestsUseSessionTokenWhenAvailable() {
        val auth = Fetch.resolveApiAuthorization(
            sessionToken = "session-token",
            userId = null,
            unionId = "wechat-union-id",
        )

        assertTrue(auth.usedSessionToken)
        assertEquals("Bearer session-token", auth.headerValue)
    }

    @Test
    fun userScopedRequestsFallBackToAppTokenWhenSessionMissing() {
        val auth = Fetch.resolveApiAuthorization(
            sessionToken = "",
            userId = "ftc-user-id",
            unionId = null,
        )

        assertFalse(auth.usedSessionToken)
        assertEquals("Bearer ${Endpoint.accessToken}", auth.headerValue)
    }

    @Test
    fun legacyUserScopedRequestsAlwaysKeepUsingAppToken() {
        val auth = Fetch.resolveLegacyApiAuthorization()

        assertFalse(auth.usedSessionToken)
        assertEquals("Bearer ${Endpoint.accessToken}", auth.headerValue)
    }

    @Test
    fun accountRefreshUnauthorizedStillForcesLogoutWithoutSessionToken() {
        val shouldLogout = Fetch.shouldForceLogoutOnRejectedSession(
            requestUsedSessionToken = false,
            userId = "ftc-user-id",
            path = "/api/account",
        )

        assertTrue(shouldLogout)
    }

    @Test
    fun nonAccountUnauthorizedDoesNotForceLogoutWhenSessionTokenWasNotUsed() {
        val shouldLogout = Fetch.shouldForceLogoutOnRejectedSession(
            requestUsedSessionToken = false,
            userId = "ftc-user-id",
            path = "/api/stripe/subs/sub_123/refresh",
        )

        assertFalse(shouldLogout)
    }

    @Test
    fun sessionBackedRequestsStillForceLogoutOutsideAccountRefresh() {
        val shouldLogout = Fetch.shouldForceLogoutOnRejectedSession(
            requestUsedSessionToken = true,
            userId = "ftc-user-id",
            path = "/api/stripe/subs/sub_123/refresh",
        )

        assertTrue(shouldLogout)
    }
}
