package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CatalogItem
import com.example.data.model.Episode
import com.example.data.model.Season
import com.example.data.model.SeasonDraft
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

    @Test
    fun admin_canSaveAndLoadTvShowWithSeasonsAndEpisodes_supportingYoutubeMp4AndHls() = runBlocking {
        signInVerifiedAdminUser()
        val catalogRepo = CatalogRepository(firestore, auth)

        val series = CatalogItem(
            id = "admin-series-${UUID.randomUUID()}",
            title = "Notflix Original Series",
            synopsis = "Multi-source TV show saved by admin.",
            mediaType = "series",
            genres = listOf("Sci-Fi", "Drama"),
            releaseYear = 2026,
            runtimeMinutes = 48,
            ageRating = "TV-14",
            director = "Showrunner Name",
            cast = listOf("Actor Alpha", "Actor Beta"),
            posterUrl = "https://example.com/poster.jpg",
            backdropUrl = "https://example.com/backdrop.jpg",
            trailerUrl = "https://www.youtube.com/watch?v=aqz-KE-bpKQ",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            videoSourceType = "mp4",
            isFeatured = true,
            isVisible = true,
            isKidsFriendly = false
        )

        val seasons = listOf(
            SeasonDraft(
                season = Season(id = "season-1", seasonNumber = 1, title = "Season 1", year = 2026, isVisible = true),
                episodes = listOf(
                    Episode(
                        id = "s1e1",
                        seasonNumber = 1,
                        episodeNumber = 1,
                        title = "Episode One (YouTube)",
                        synopsis = "YouTube embed episode",
                        runtimeMinutes = 42,
                        videoUrl = "https://www.youtube.com/watch?v=aqz-KE-bpKQ",
                        videoSourceType = "youtube",
                        isVisible = true
                    ),
                    Episode(
                        id = "s1e2",
                        seasonNumber = 1,
                        episodeNumber = 2,
                        title = "Episode Two (HLS)",
                        synopsis = "HLS adaptive stream episode",
                        runtimeMinutes = 46,
                        videoUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                        videoSourceType = "hls",
                        hlsUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                        isVisible = true
                    )
                )
            )
        )

        val savedId = withTimeout(DEFAULT_TIMEOUT_MS) {
            catalogRepo.saveSeriesWithSeasons(series = series, seasonDrafts = seasons).getOrThrow()
        }
        assertEquals(series.id, savedId)

        val loadedStructure = withTimeout(DEFAULT_TIMEOUT_MS) {
            catalogRepo.loadSeriesStructure(savedId).getOrThrow()
        }
        assertEquals(1, loadedStructure.size)
        assertEquals(2, loadedStructure.first().episodes.size)
        assertEquals("youtube", loadedStructure.first().episodes[0].videoSourceType)
        assertEquals("hls", loadedStructure.first().episodes[1].videoSourceType)
        assertEquals("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", loadedStructure.first().episodes[1].preferredStreamUrl)
    }

    @Test
    fun regularMember_cannotSaveTvShowOrEpisodes_failsWithPermissionDenied() = runBlocking {
        signInTestUser("member_series_${UUID.randomUUID()}@velora.test")
        val catalogRepo = CatalogRepository(firestore, auth)

        val attempt = withTimeout(DEFAULT_TIMEOUT_MS) {
            catalogRepo.saveSeriesWithSeasons(
                series = CatalogItem(
                    id = "unauth-series-${UUID.randomUUID()}",
                    title = "Unauthorized Series",
                    synopsis = "Members cannot publish shows",
                    mediaType = "series",
                    genres = listOf("Drama"),
                    releaseYear = 2026,
                    runtimeMinutes = 40,
                    ageRating = "TV-14",
                    director = "Member",
                    videoUrl = "https://www.youtube.com/watch?v=aqz-KE-bpKQ",
                    videoSourceType = "youtube"
                ),
                seasonDrafts = listOf(
                    SeasonDraft(
                        season = Season(id = "season-1", seasonNumber = 1, title = "Season 1", year = 2026),
                        episodes = listOf(
                            Episode(
                                id = "s1e1",
                                seasonNumber = 1,
                                episodeNumber = 1,
                                title = "Pilot",
                                synopsis = "Should be denied",
                                runtimeMinutes = 40,
                                videoUrl = "https://www.youtube.com/watch?v=aqz-KE-bpKQ",
                                videoSourceType = "youtube"
                            )
                        )
                    )
                )
            )
        }
        assertTrue(attempt.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            attempt.exceptionOrNull()?.unwrapFirestoreException()?.code
        )
    }

    @Test
    fun admin_canInitializeAndLoadAllUsersForAdminDashboard_whileMemberCannotSelfPromote() = runBlocking {
        val memberEmail = "member_escalate_${UUID.randomUUID()}@velora.test"
        val memberUid = signInTestUser(memberEmail)
        val memberAuthRepo = AuthRepository(firestore, auth)
        val memberCatalogRepo = CatalogRepository(firestore, auth)

        val memberAccount = withTimeout(DEFAULT_TIMEOUT_MS) {
            memberAuthRepo.ensureAccountInitialized().getOrThrow()
        }
        assertEquals("member", memberAccount.role)

        val promoteAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
            memberCatalogRepo.setUserAdminRole(memberAccount, makeAdmin = true)
        }
        assertTrue(promoteAttempt.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            promoteAttempt.exceptionOrNull()?.unwrapFirestoreException()?.code
        )

        val adminUid = signInVerifiedAdminUser()
        val adminAuthRepo = AuthRepository(firestore, auth)
        val adminCatalogRepo = CatalogRepository(firestore, auth)

        val initializedAdmin = withTimeout(DEFAULT_TIMEOUT_MS) {
            adminAuthRepo.ensureAccountInitialized().getOrThrow()
        }
        assertEquals(adminUid, initializedAdmin.userId)
        assertEquals("admin", initializedAdmin.role)

        val observedUsers = withTimeout(FLOW_TIMEOUT_MS) {
            adminCatalogRepo.observeAllUsersForAdmin().first { users ->
                users.any { it.userId == adminUid } && users.any { it.userId == memberUid }
            }
        }
        assertTrue(observedUsers.any { it.userId == memberUid && it.role == "member" })
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 8000L
        const val FLOW_TIMEOUT_MS = 3500L
    }
}
