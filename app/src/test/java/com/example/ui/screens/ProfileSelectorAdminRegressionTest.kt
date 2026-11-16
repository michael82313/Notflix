package com.example.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.model.CatalogItem
import com.example.data.model.Genre
import com.example.data.model.UiState
import com.example.data.model.UserAccount
import com.example.data.model.UserProfile
import com.example.ui.theme.VeloraTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProfileSelectorAdminRegressionTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun clickingAdminOnProfileSelector_opensAdminConsoleWithoutCrashing() {
        val sampleProfiles = listOf(
            UserProfile(id = "main", userId = "admin_1", name = "Main", avatarKey = "crimson")
        )
        val sampleCatalog = listOf(
            CatalogItem(
                id = "movie-1",
                title = "Vimeo Showcase",
                synopsis = "Embedded multi-host showcase",
                mediaType = "movie",
                genres = listOf("Drama"),
                releaseYear = 2026,
                runtimeMinutes = 90,
                ageRating = "PG-13",
                videoUrl = "https://vimeo.com/76979871",
                videoSourceType = "auto",
                providerName = "Vimeo"
            )
        )
        val sampleUsers = listOf(
            UserAccount(userId = "admin_1", email = "michael.tollackson82313@gmail.com", displayName = "Admin", role = "admin"),
            UserAccount(userId = "", email = "legacy@example.com", displayName = "Legacy Blank UID", role = "member")
        )

        composeRule.setContent {
            var showPreProfileAdmin by remember { mutableStateOf(false) }
            VeloraTheme {
                if (showPreProfileAdmin) {
                    AdminDashboardScreen(
                        isAdmin = true,
                        catalog = sampleCatalog,
                        genres = listOf(Genre(id = "drama", name = "Drama")),
                        users = sampleUsers,
                        subscriptionPlans = emptyList(),
                        userSubscriptions = emptyList(),
                        isSavingCatalog = false,
                        isLoadingSeriesSeasons = false,
                        loadedSeriesSeasons = emptyList(),
                        onBack = { showPreProfileAdmin = false },
                        onSeedCatalog = {},
                        onSyncSubscriptionSchemas = {},
                        onPrepareSeriesEditor = {},
                        onSaveCatalogItem = { _, done -> done() },
                        onSaveSeriesWithSeasons = { _, _, _, _, done -> done() },
                        onDeleteCatalogItem = { _, _ -> },
                        onAddSeason = { _, _ -> },
                        onAddEpisode = { _, _, _ -> },
                        onSaveGenre = {},
                        onDeleteGenre = {},
                        onToggleUserAdmin = { _, _ -> }
                    )
                } else {
                    ProfileSelectorScreen(
                        profilesState = UiState.Success(sampleProfiles),
                        genres = emptyList(),
                        isAdmin = true,
                        accountIdentifier = "michael.tollackson82313@gmail.com",
                        onSelectProfile = {},
                        onOpenAdminDashboard = { showPreProfileAdmin = true },
                        onCreateProfile = { _, _, _, _ -> },
                        onUpdateProfile = { _, _, _, _, _, _, _ -> },
                        onDeleteProfile = {}
                    )
                }
            }
        }

        composeRule.onNodeWithTag("profile_selector_admin_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Admin Console").assertIsDisplayed()
        composeRule.onNode(hasText("Users") and isSelectable()).performClick()
        composeRule.onNodeWithText("Legacy Blank UID").assertIsDisplayed()
        composeRule.onNode(hasText("Catalog (1)") and isSelectable()).performClick()
        composeRule.onNodeWithText("Vimeo Showcase").assertIsDisplayed()
    }

    @Test
    fun nonAdminUser_doesNotSeeAdminButtonAndCannotAccessAdminDashboard() {
        composeRule.setContent {
            VeloraTheme {
                ProfileSelectorScreen(
                    profilesState = UiState.Success(listOf(UserProfile(id = "p1", name = "Viewer"))),
                    genres = emptyList(),
                    isAdmin = false,
                    onSelectProfile = {},
                    onOpenAdminDashboard = {},
                    onCreateProfile = { _, _, _, _ -> },
                    onUpdateProfile = { _, _, _, _, _, _, _ -> },
                    onDeleteProfile = {}
                )
            }
        }
        composeRule.onNodeWithTag("profile_selector_admin_button").assertDoesNotExist()
    }
}
