package com.lumokato.nunulo

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiTest {
    @Test
    fun authorizedRequestAddsBearerAndBuildsExactUrl() {
        val request = JsonHttpClient()
            .authorizedBuilder("https://nunulo.test/", "/api/private", "access-token")
            .get()
            .build()

        assertEquals("https://nunulo.test/api/private", request.url.toString())
        assertEquals("Bearer access-token", request.header("Authorization"))
    }

    @Test
    fun authenticationExpiryUsesStatusInsteadOfMessageGuessing() {
        assertTrue(looksLikeExpiredToken(ApiException(401, "任何服务端消息")))
        assertFalse(looksLikeExpiredToken(ApiException(503, "token storage unavailable")))
        assertFalse(looksLikeExpiredToken(IllegalStateException("HTTP 401")))
    }

    @Test
    fun apiUrlNormalizesBaseWithoutChangingPath() {
        assertEquals("https://nunulo.test/api/checkins?limit=120", apiUrl(" https://nunulo.test/// ", "/api/checkins?limit=120"))
    }

    @Test
    fun concurrentExpiredRequestsShareOneRefresh() = runBlocking {
        val coordinator = TokenRefreshCoordinator()
        var accessToken = "expired"
        var refreshCalls = 0
        val bothExpired = CompletableDeferred<Unit>()
        var expiredCalls = 0

        suspend fun request(token: String): String {
            if (token == "expired") {
                expiredCalls += 1
                if (expiredCalls == 2) bothExpired.complete(Unit)
                bothExpired.await()
                throw ApiException(401, "访问令牌已失效")
            }
            return token
        }

        val results = listOf(
            async {
                coordinator.run(
                    currentAccessToken = { accessToken },
                    currentRefreshToken = { "refresh-token" },
                    persistAccessToken = { accessToken = it },
                    refreshAccessToken = { refreshCalls += 1; "renewed" },
                    block = ::request,
                )
            },
            async {
                coordinator.run(
                    currentAccessToken = { accessToken },
                    currentRefreshToken = { "refresh-token" },
                    persistAccessToken = { accessToken = it },
                    refreshAccessToken = { refreshCalls += 1; "renewed" },
                    block = ::request,
                )
            },
        ).awaitAll()

        assertEquals(listOf("renewed", "renewed"), results)
        assertEquals(1, refreshCalls)
        assertEquals("renewed", accessToken)
    }

    @Test
    fun nonAuthenticationFailureIsNotRetried() = runBlocking {
        val coordinator = TokenRefreshCoordinator()
        val expected = ApiException(503, "storage unavailable")
        var refreshCalls = 0

        val actual = runCatching {
            coordinator.run(
                currentAccessToken = { "access" },
                currentRefreshToken = { "refresh" },
                persistAccessToken = {},
                refreshAccessToken = { refreshCalls += 1; "renewed" },
                block = { throw expected },
            )
        }.exceptionOrNull()

        assertSame(expected, actual)
        assertEquals(0, refreshCalls)
    }
}
