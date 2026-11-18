package com.example.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreDocumentMapperTest {

    @Test
    fun toUserAccount_handlesExplicitNullsMissingFieldsAndWrongTypesWithoutCrashing() {
        val malformed = mapOf<String, Any?>(
            "id" to "legacy_id_ignored",
            "userId" to null,
            "email" to null,
            "displayName" to null,
            "role" to null,
            "isSuspended" to "true",
            "autoplayNext" to null,
            "createdAt" to "not_a_timestamp",
            "updatedAt" to 12345L
        )

        val account = FirestoreDocumentMapper.toUserAccount("uid_abc", malformed)
        assertEquals("uid_abc", account.id)
        assertEquals("uid_abc", account.userId)
        assertEquals("", account.email)
        assertEquals("", account.displayName)
        assertEquals("viewer", account.role)
        assertEquals(com.example.data.model.AppRole.VIEWER, account.parsedRole)
        assertTrue(account.isSuspended)
        assertTrue(account.autoplayNext)
        assertNull(account.createdAt)
        assertNull(account.updatedAt)
    }

    @Test
    fun toCatalogItem_handlesNullListsNumericStringsAndMissingUrlsSafely() {
        val malformed = mapOf<String, Any?>(
            "id" to "doc_override_test",
            "title" to null,
            "synopsis" to null,
            "mediaType" to "",
            "genres" to listOf("Sci-Fi", null, 42, ""),
            "releaseYear" to "2026",
            "runtimeMinutes" to 114L,
            "cast" to "not_a_list",
            "videoUrl" to null,
            "posterUrl" to "   ",
            "isFeatured" to "false",
            "isVisible" to null,
            "trendingScore" to "bad_int"
        )

        val item = FirestoreDocumentMapper.toCatalogItem("cat_123", malformed)
        assertEquals("cat_123", item.id)
        assertEquals("Untitled Title", item.title)
        assertEquals("movie", item.mediaType)
        assertEquals(listOf("Sci-Fi", "42"), item.genres)
        assertEquals(2026, item.releaseYear)
        assertEquals(114, item.runtimeMinutes)
        assertEquals(emptyList<String>(), item.cast)
        assertEquals("", item.videoUrl)
        assertNull(item.posterUrl)
        assertFalse(item.isFeatured)
        assertTrue(item.isVisible)
        assertEquals(80, item.trendingScore)
    }

    @Test
    fun toUserProfileAndSubscription_handleLegacyNullsAndMissingKeysSafely() {
        val profile = FirestoreDocumentMapper.toUserProfile(
            "p_1",
            mapOf(
                "userId" to null,
                "name" to null,
                "preferredGenres" to null,
                "isKids" to null
            )
        )
        assertEquals("p_1", profile.id)
        assertEquals("Viewer", profile.name)
        assertEquals(emptyList<String>(), profile.preferredGenres)
        assertFalse(profile.isKids)

        val sub = FirestoreDocumentMapper.toUserSubscription(
            "uid_xyz",
            mapOf(
                "userId" to null,
                "planName" to null,
                "status" to null,
                "assignedBy" to null
            )
        )
        assertEquals("uid_xyz", sub.userId)
        assertEquals("Free", sub.planName)
        assertEquals("active", sub.status)
        assertEquals("system", sub.assignedBy)
    }
}
