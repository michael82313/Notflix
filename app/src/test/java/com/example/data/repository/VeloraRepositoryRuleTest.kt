package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CatalogItem
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class VeloraRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun authenticatedUser_canInitializeAccountAndCreateProfileAndWatchlist() = runBlocking {
        val uid = signInTestUser("alice_${UUID.randomUUID()}@velora.test")
        val authRepo = AuthRepository(firestore, auth)
        val profileRepo = ProfileRepository(firestore, auth)

        val account = withTimeout(DEFAULT_TIMEOUT_MS) {
            authRepo.ensureAccountInitialized().getOrThrow()
        }
        assertEquals(uid, account.userId)
        assertEquals("member", account.role)

        val profileId = withTimeout(DEFAULT_TIMEOUT_MS) {
            profileRepo.createProfile(
                name = "Cinema Night",
                avatarKey = "crimson",
                isKids = false,
                preferredGenres = listOf("Sci-Fi", "Thriller")
            ).getOrThrow()
        }
        assertTrue(profileId.isNotBlank())

        val profiles = withTimeout(FLOW_TIMEOUT_MS) {
            profileRepo.observeProfiles(uid).first { it.any { p -> p.id == profileId } }
        }
        assertTrue(profiles.any { it.id == profileId && it.name == "Cinema Night" })

        val sampleItem = CatalogItem(
            id = "tears-of-steel",
            title = "Tears of Steel",
            mediaType = "movie"
        )
        val added = withTimeout(DEFAULT_TIMEOUT_MS) {
            profileRepo.toggleWatchlist(profileId, sampleItem, currentlyInList = false).getOrThrow()
        }
        assertTrue(added)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            profileRepo.savePlaybackProgress(
                profileId = profileId,
                contentId = "tears-of-steel",
                episodeId = null,
                seasonNumber = null,
                episodeNumber = null,
                title = "Tears of Steel",
                positionMs = 120_000L,
                durationMs = 720_000L
            ).getOrThrow()
        }

        val progress = withTimeout(FLOW_TIMEOUT_MS) {
            profileRepo.observeProgress(uid, profileId).first { it.isNotEmpty() }
        }
        assertEquals(120_000L, progress.first().positionMs)
    }

    @Test
    fun crossUserAccess_readingAnotherUsersProfiles_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice_iso_${UUID.randomUUID()}@velora.test")
        val aliceProfileRepo = ProfileRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceProfileRepo.createProfile(
                name = "Alice Private",
                avatarKey = "amber",
                isKids = false,
                preferredGenres = listOf("Drama")
            ).getOrThrow()
        }

        signInTestUser("bob_iso_${UUID.randomUUID()}@velora.test")
        val bobProfileRepo = ProfileRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobProfileRepo.observeProfiles(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED when reading another user's profiles")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    @Test
    fun unauthenticatedUser_observeCatalog_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val catalogRepo = CatalogRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                catalogRepo.observeCatalog(includeHiddenForAdmin = false).first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated catalog query")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    @Test
    fun regularMember_cannotWriteToCatalog_failsWithPermissionDenied() = runBlocking {
        signInTestUser("member_${UUID.randomUUID()}@velora.test")
        val catalogRepo = CatalogRepository(firestore, auth)
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            catalogRepo.saveCatalogItem(
                CatalogItem(
                    title = "Unauthorized Movie",
                    synopsis = "Should fail",
                    mediaType = "movie",
                    genres = listOf("Action"),
                    releaseYear = 2025,
                    runtimeMinutes = 90,
                    ageRating = "PG",
                    director = "User",
                    cast = listOf("Actor"),
                    videoUrl = "https://example.com/v.mp4"
                )
            )
        }
        assertTrue(result.isFailure)
        val firestoreEx = result.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun authenticatedUser_autoEnrollsInPermanentFreePlan_andCannotReadOtherUserSubscription() = runBlocking {
        val aliceUid = signInTestUser("alice_sub_${UUID.randomUUID()}@velora.test")
        val aliceSubRepo = SubscriptionRepository(firestore, auth)

        val enrolled = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceSubRepo.ensureFreeSubscription().getOrThrow()
        }
        assertEquals("free", enrolled.planId)
        assertEquals("Free", enrolled.planName)
        assertEquals("active", enrolled.status)
        assertTrue(enrolled.isPermanentFree)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            aliceSubRepo.observeUserSubscription(aliceUid).first { it.userId == aliceUid }
        }
        assertEquals("free", observed.planId)

        signInTestUser("bob_sub_${UUID.randomUUID()}@velora.test")
        val bobSubRepo = SubscriptionRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobSubRepo.observeUserSubscription(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED reading another user's subscription")
        } catch (e: Exception) {
            val firestoreEx = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }

        val seedAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobSubRepo.seedSubscriptionArchitectureIfEmpty()
        }
        assertTrue(seedAttempt.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            seedAttempt.exceptionOrNull()?.unwrapFirestoreException()?.code
        )
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3500L
    }
}
