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
import org.json.JSONArray
import org.json.JSONObject

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
    fun feedScopesBuildExactMultiUserUrls() {
        assertEquals("/api/checkins?scope=all&limit=120&offset=0", checkinFeedPath(FeedScope.All))
        assertEquals("/api/checkins?scope=following&limit=120&offset=0", checkinFeedPath(FeedScope.Following))
        assertEquals("/api/checkins?scope=public&limit=120&offset=0", checkinFeedPath(FeedScope.Public))
        assertEquals("/api/checkins?scope=mine&limit=120&offset=0", checkinFeedPath(FeedScope.Mine))
    }

    @Test
    fun newUploadOnlyAppearsInCompatibleFeedScope() {
        assertTrue(shouldShowOwnUpload(FeedScope.All, "private"))
        assertTrue(shouldShowOwnUpload(FeedScope.Mine, "followers"))
        assertTrue(shouldShowOwnUpload(FeedScope.Public, "public"))
        assertFalse(shouldShowOwnUpload(FeedScope.Public, "private"))
        assertFalse(shouldShowOwnUpload(FeedScope.Following, "public"))
    }

    @Test
    fun parsesSharedCheckinPermissionsAndInteraction() {
        val record = parseCheckin(
            JSONObject()
                .put("id", "checkin-1")
                .put("user_id", 42)
                .put("author", JSONObject().put("display_name", "测试成员乙"))
                .put("place_name", "外滩")
                .put("note", "多人测试")
                .put("latitude", 39.901568)
                .put("longitude", 116.422600)
                .put("tags", JSONArray().put("娃娃").put("夜景"))
                .put("source", "android_capture")
                .put("visibility", "public")
                .put("can_edit", false)
                .put("interaction", JSONObject().put("liked", true).put("like_count", 3).put("comment_count", 2))
                .put("thumb_url", "/api/media/thumb")
                .put("display_url", "/api/media/display")
        )

        assertEquals(42, record.userId)
        assertEquals("测试成员乙", record.authorName)
        assertEquals("public", record.visibility)
        assertFalse(record.canEdit)
        assertTrue(record.liked)
        assertEquals(3, record.likeCount)
        assertEquals(2, record.commentCount)
        assertEquals(listOf("娃娃", "夜景"), record.tags)
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
