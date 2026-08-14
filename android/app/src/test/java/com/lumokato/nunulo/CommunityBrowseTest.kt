package com.lumokato.nunulo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONObject

class CommunityBrowseTest {
    private val people = listOf(
        PersonItem(
            id = 1,
            displayName = "小灯的旅行册",
            username = "tomori_trip",
            bio = "带着棉花娃娃去看 MyGO!!!!! 演出",
            following = true,
            followerCount = 28,
        ),
        PersonItem(
            id = 2,
            displayName = "爱音今天也出门",
            username = "anon_go",
            bio = "记录城市夜景和面基",
            following = false,
            followerCount = 12,
        ),
        PersonItem(
            id = 3,
            displayName = "旅行收藏者",
            username = "live_archive",
            bio = "演出与伙伴合照",
            following = true,
            followerCount = 42,
        ),
    )

    @Test
    fun searchesDisplayNameUsernameAndBio() {
        assertEquals(listOf(2), communityBrowseItems(people, "爱音", CommunityFollowFilter.All).map(PersonItem::id))
        assertEquals(listOf(1), communityBrowseItems(people, "TOMORI", CommunityFollowFilter.All).map(PersonItem::id))
        assertEquals(listOf(3, 1), communityBrowseItems(people, "演出", CommunityFollowFilter.All).map(PersonItem::id))
    }

    @Test
    fun followingFilterKeepsOnlyFollowedMembersAndUsesRealFollowerOrder() {
        val result = communityBrowseItems(people, "", CommunityFollowFilter.Following)

        assertEquals(listOf(3, 1), result.map(PersonItem::id))
        assertTrue(result.all(PersonItem::following))
    }

    @Test
    fun parsesMemberProfileFieldsWithoutRequiringNewApiFields() {
        val current = parsePerson(
            JSONObject()
                .put("id", 8)
                .put("display_name", "测试成员")
                .put("username", "member")
                .put("bio", "一起去看演出")
                .put("following", true)
                .put("avatar_url", "/api/assets/avatar/content")
                .put("follower_count", 9)
                .put("following_count", 4),
        )
        val legacy = parsePerson(JSONObject().put("id", 9).put("display_name", "旧接口成员"))

        assertEquals("/api/assets/avatar/content", current.avatarUrl)
        assertEquals(9, current.followerCount)
        assertEquals(4, current.followingCount)
        assertTrue(current.following)
        assertEquals(null, legacy.avatarUrl)
        assertEquals(0, legacy.followerCount)
        assertFalse(legacy.following)
    }

    @Test
    fun memberProfileRejectsRecordsFromAnApiThatIgnoresAuthorFilter() {
        fun record(id: String, userId: Int) = CheckinItem(
            id = id,
            userId = userId,
            placeName = "",
            note = "",
            latitude = null,
            longitude = null,
            createdAt = null,
            takenAt = null,
            source = "android_capture",
        )
        val memberRecord = record("member", 8)
        val unrelatedRecord = record("other", 9)

        assertEquals(listOf(memberRecord), memberRecordsFor(8, listOf(unrelatedRecord, memberRecord)))
    }

    @Test
    fun followNotificationTargetsTheFollowerProfile() {
        val follow = NotificationItem("n1", "新的关注", "旅行收藏者关注了你", "user", "42", null, null)
        val malformed = follow.copy(targetId = "not-a-user")
        val record = follow.copy(targetType = "checkin", targetId = "42")

        assertEquals(42, notificationTargetUserId(follow))
        assertEquals(null, notificationTargetUserId(malformed))
        assertEquals(null, notificationTargetUserId(record))
    }
}
