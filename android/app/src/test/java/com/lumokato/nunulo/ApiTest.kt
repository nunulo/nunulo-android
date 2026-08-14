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
    fun feedScopesAndOrderBuildExactProductUrls() {
        assertEquals("/api/checkins?scope=discover&order=popular&limit=120&offset=0", checkinFeedPath(FeedScope.Discover, FeedOrder.Popular))
        assertEquals("/api/checkins?scope=following&order=latest&limit=120&offset=0", checkinFeedPath(FeedScope.Following, FeedOrder.Latest))
        assertEquals("/api/checkins?scope=mine&order=latest&limit=120&offset=0&partner_id=partner-1", checkinFeedPath(FeedScope.Mine, FeedOrder.Latest, mapOf("partner_id" to "partner-1")))
    }

    @Test
    fun parsesMultiPhotoRecordRelationsPermissionsAndInteraction() {
        val record = parseCheckin(
            JSONObject()
                .put("id", "checkin-1")
                .put("user_id", 42)
                .put("author", JSONObject().put("display_name", "测试成员乙"))
                .put("photo_ids", JSONArray().put("photo-1").put("photo-2"))
                .put(
                    "photos",
                    JSONArray()
                        .put(JSONObject().put("id", "photo-1").put("display_url", "/api/assets/display-1").put("thumb_url", "/api/assets/thumb-1"))
                        .put(JSONObject().put("id", "photo-2").put("display_url", "/api/assets/display-2").put("thumb_url", "/api/assets/thumb-2")),
                )
                .put("place_name", "外滩")
                .put("note", "多人测试")
                .put("latitude", 39.901568)
                .put("longitude", 116.422600)
                .put("source", "android_capture")
                .put("visibility", "public")
                .put("world_visible", true)
                .put("public_showcase", true)
                .put("location_source", "photo_exif")
                .put("location_privacy", "regional")
                .put(
                    "catalog",
                    JSONObject()
                        .put("item_types", JSONArray().put(JSONObject().put("id", "type-1").put("canonical_name", "棉花娃娃")))
                        .put("works", JSONArray().put(JSONObject().put("id", "work-1").put("canonical_name", "BanG Dream!")))
                        .put("characters", JSONArray().put(JSONObject().put("id", "character-1").put("canonical_name", "户山香澄"))),
                )
                .put(
                    "partners",
                    JSONArray().put(
                        JSONObject()
                            .put("id", "partner-1")
                            .put("public_code", "N-ABC")
                            .put("owner_user_id", 42)
                            .put("name", "香澄")
                            .put("visibility", "public")
                            .put("moderation_status", "active")
                            .put("record_count", 1)
                            .put("can_edit", true),
                    ),
                )
                .put(
                    "events",
                    JSONArray().put(
                        JSONObject()
                            .put("id", "event-1")
                            .put("name", "Live")
                            .put("event_type", "offline_live")
                            .put("status", "active")
                            .put("official", false),
                    ),
                )
                .put("can_edit", false)
                .put("interaction", JSONObject().put("liked", true).put("like_count", 3).put("comment_count", 2))
                .put("thumb_url", "/api/media/thumb")
                .put("display_url", "/api/media/display")
        )

        assertEquals(42, record.userId)
        assertEquals("测试成员乙", record.authorName)
        assertEquals("public", record.visibility)
        assertTrue(record.worldVisible)
        assertTrue(record.publicShowcase)
        assertFalse(record.canEdit)
        assertTrue(record.liked)
        assertEquals(3, record.likeCount)
        assertEquals(2, record.commentCount)
        assertEquals(listOf("photo-1", "photo-2"), record.photoIds)
        assertEquals(listOf("棉花娃娃", "BanG Dream!", "户山香澄"), record.taxonomyNames)
        assertEquals("N-ABC", record.partners.single().publicCode)
        assertEquals("offline_live", record.events.single().eventType)
    }

    @Test
    fun partnerRequestParserKeepsHumanNamesForConfirmationUi() {
        val request = parsePartnerRequest(
            JSONObject()
                .put("checkin_id", "checkin-1")
                .put("partner_id", "partner-1")
                .put("partner_name", "高松灯棉花娃娃")
                .put("partner_code", "N-2026-08-000128")
                .put("record_author_user_id", 7)
                .put("record_author_display_name", "旅行收藏者")
                .put("partner_owner_user_id", 42)
                .put("partner_owner_display_name", "小灯的主人")
                .put("author_approved", true)
                .put("owner_approved", false)
        )

        assertEquals("旅行收藏者", request.recordAuthorDisplayName)
        assertEquals("小灯的主人", request.partnerOwnerDisplayName)
    }

    @Test
    fun checkinPayloadUsesPhotoIdsRelationsAndIndependentVisibility() {
        val payload = checkinPayload(
            UploadDraft(
                photos = listOf(
                    DraftPhotoItem(photo = PhotoItem("photo-2"), status = "ready"),
                    DraftPhotoItem(photo = PhotoItem("photo-1"), status = "ready"),
                ),
                placeName = "东京巨蛋",
                latitude = "35.7056",
                longitude = "139.7519",
                locationSource = "photo_exif",
                locationProvider = "exif",
                locationCapturedAtMillis = 1_786_640_000_000,
                locationAccuracyMeters = 18.5f,
                locationPrivacy = "regional",
                visibility = "public",
                worldVisible = true,
                publicShowcase = true,
                partnerIds = listOf("partner-1", "partner-2"),
                itemTypeIds = listOf("type-1"),
                workIds = listOf("work-1"),
                characterIds = listOf("character-1"),
                eventIds = listOf("event-1"),
            ),
            "request-1",
        )

        assertEquals(listOf("photo-2", "photo-1"), payload.getJSONArray("photo_ids").let { array -> List(array.length()) { array.getString(it) } })
        assertEquals("request-1", payload.getString("client_request_id"))
        assertEquals("photo_exif", payload.getString("location_source"))
        assertEquals("exif", payload.getString("location_provider"))
        assertEquals("2026-08-13T16:53:20Z", payload.getString("location_captured_at"))
        assertEquals(18.5, payload.getDouble("location_accuracy_meters"), 0.001)
        assertEquals("regional", payload.getString("location_privacy"))
        assertTrue(payload.getBoolean("world_visible"))
        assertTrue(payload.getBoolean("public_showcase"))
        assertEquals(2, payload.getJSONArray("partner_ids").length())
        assertFalse(payload.has("tags"))
    }

    @Test
    fun draftValidationAllowsNoLocationButRequiresOneToNineReadyPhotos() {
        val valid = validateDraft(UploadDraft(photos = listOf(DraftPhotoItem(photo = PhotoItem("photo-1"), status = "ready"))))
        val partialCoordinates = validateDraft(UploadDraft(photos = listOf(DraftPhotoItem(photo = PhotoItem("photo-1"), status = "ready")), latitude = "39"))
        val failedPhoto = validateDraft(UploadDraft(photos = listOf(DraftPhotoItem(status = "error"))))

        assertTrue(valid.ready)
        assertFalse(partialCoordinates.ready)
        assertFalse(failedPhoto.ready)
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
