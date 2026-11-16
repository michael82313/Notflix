package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.model.CatalogItem
import com.example.data.model.Genre
import com.example.data.model.SubscriptionCatalogDefaults
import com.example.data.model.UiState
import com.example.data.model.UserAccount
import com.example.data.model.UserProfile
import com.example.data.model.UserSubscription
import com.example.ui.navigation.VeloraRoute
import com.example.ui.theme.VeloraTheme
import org.junit.Assert.assertTrue
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
    fun bootstrappedAdmin_canOpenAdminFromProfileSelectorSwitchTabsAndNavigateBackRepeatedly() {
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
            ),
            CatalogItem(
                id = "movie-blank-fields",
                title = "Minimal Doc Movie",
                synopsis = "",
                mediaType = "movie",
                genres = emptyList(),
                releaseYear = 2025,
                runtimeMinutes = 60,
                ageRating = "",
                videoUrl = "",
                videoSourceType = "",
                providerName = null
            )
        )
        val sampleUsers = listOf(
            UserAccount(userId = "admin_1", email = "michael.tollackson82313@gmail.com", displayName = "Michael", role = "admin"),
            UserAccount(userId = "", email = "", displayName = "", role = "")
        )
        val samplePlans = SubscriptionCatalogDefaults.allBlueprintTiers
        val sampleSubs = listOf(
            UserSubscription(id = "sub_1", userId = "", planName = "", status = "", assignedBy = "")
        )
        var selectedProfileCount = 0

        composeRule.setContent {
            var hasSelectedProfile by remember { mutableStateOf(false) }
            var showPreProfileAdmin by remember { mutableStateOf(false) }
            VeloraTheme {
                if (!hasSelectedProfile) {
                    if (showPreProfileAdmin) {
                        AdminDashboardScreen(
                            isAdmin = true,
                            catalog = sampleCatalog,
                            genres = listOf(Genre(id = "drama", name = "Drama"), Genre(id = "", name = "Sci-Fi")),
                            users = sampleUsers,
                            subscriptionPlans = samplePlans,
                            userSubscriptions = sampleSubs,
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
                            onSelectProfile = {
                                selectedProfileCount++
                                hasSelectedProfile = true
                            },
                            onOpenAdminDashboard = { showPreProfileAdmin = true },
                            onCreateProfile = { _, _, _, _ -> },
                            onUpdateProfile = { _, _, _, _, _, _, _ -> },
                            onDeleteProfile = {}
                        )
                    }
                } else {
                    Box(Modifier.fillMaxSize())
                }
            }
        }

        // Open Admin Console first time
        composeRule.onNodeWithTag("profile_selector_admin_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Admin Console").assertIsDisplayed()

        // Navigate across all 5 tabs including Users & Plans with incomplete/blank fields
        composeRule.onNode(hasText("Catalog (2)") and isSelectable()).performClick()
        composeRule.onNodeWithText("Vimeo Showcase").assertIsDisplayed()
        composeRule.onNode(hasText("Genres") and isSelectable()).performClick()
        composeRule.onNodeWithText("Sci-Fi").assertIsDisplayed()
        composeRule.onNode(hasText("Users") and isSelectable()).performClick()
        composeRule.onNodeWithText("Notflix User").assertIsDisplayed()
        composeRule.onNode(hasText("Plans") and isSelectable()).performClick()
        composeRule.onNodeWithText("Free-First Architecture").assertIsDisplayed()

        // Go back to Profile Selector and re-open Admin a second time
        composeRule.onNodeWithTag("admin_back_button").performClick()
        composeRule.onNodeWithTag("profile_selector_admin_button").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Admin Console").assertIsDisplayed()

        // Go back and select profile cleanly
        composeRule.onNodeWithTag("admin_back_button").performClick()
        composeRule.onNodeWithTag("profile_tile_main").performClick()
        assertTrue(selectedProfileCount == 1)
    }

    @Test
    fun secondPromotedAdmin_andPostProfileNavHostAdminRoute_workWithoutNavigationCrash() {
        composeRule.setContent {
            val navController = rememberNavController()
            var hasSelectedProfile by remember { mutableStateOf(false) }
            var showPreProfileAdmin by remember { mutableStateOf(false) }
            VeloraTheme {
                if (!hasSelectedProfile) {
                    if (showPreProfileAdmin) {
                        AdminDashboardScreen(
                            isAdmin = true,
                            catalog = emptyList(),
                            genres = emptyList(),
                            users = listOf(UserAccount(userId = "admin_2", email = "other.admin@example.com", displayName = "Co-Admin", role = "admin")),
                            subscriptionPlans = emptyList(),
                            userSubscriptions = emptyList(),
                            isSavingCatalog = false,
                            isLoadingSeriesSeasons = false,
                            loadedSeriesSeasons = emptyList(),
                            adminErrorMessage = "Simulated transient sync error",
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
                            profilesState = UiState.Success(listOf(UserProfile(id = "p2", name = "Co-Admin"))),
                            genres = emptyList(),
                            isAdmin = true,
                            accountIdentifier = "other.admin@example.com",
                            onSelectProfile = { hasSelectedProfile = true },
                            onOpenAdminDashboard = { showPreProfileAdmin = true },
                            onCreateProfile = { _, _, _, _ -> },
                            onUpdateProfile = { _, _, _, _, _, _, _ -> },
                            onDeleteProfile = {}
                        )
                    }
                } else {
                    NavHost(
                        navController = navController,
                        startDestination = VeloraRoute.Home
                    ) {
                        composable<VeloraRoute.Home> {
                            androidx.compose.material3.Button(
                                onClick = { navController.navigate(VeloraRoute.Admin) { launchSingleTop = true } },
                                modifier = Modifier.testTag("home_open_admin")
                            ) {
                                androidx.compose.material3.Text("Open Admin From Home")
                            }
                        }
                        composable<VeloraRoute.Admin> {
                            AdminDashboardScreen(
                                isAdmin = true,
                                catalog = emptyList(),
                                genres = emptyList(),
                                users = emptyList(),
                                subscriptionPlans = emptyList(),
                                userSubscriptions = emptyList(),
                                isSavingCatalog = false,
                                isLoadingSeriesSeasons = false,
                                loadedSeriesSeasons = emptyList(),
                                onBack = {
                                    if (navController.previousBackStackEntry != null) {
                                        navController.popBackStack()
                                    }
                                },
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
                        }
                    }
                }
            }
        }

        // Open pre-profile Admin with error banner surfaced safely
        composeRule.onNodeWithTag("profile_selector_admin_button").performClick()
        composeRule.onNodeWithTag("admin_error_banner").assertIsDisplayed()
        composeRule.onNodeWithText("Simulated transient sync error").assertIsDisplayed()

        // Return to Profile Selector, enter Home NavHost, open Admin & pop back twice cleanly
        composeRule.onNodeWithTag("admin_back_button").performClick()
        composeRule.onNodeWithTag("profile_tile_p2").performClick()
        composeRule.onNodeWithTag("home_open_admin").performClick()
        composeRule.onNodeWithText("Admin Console").assertIsDisplayed()
        composeRule.onNodeWithTag("admin_back_button").performClick()
        composeRule.onNodeWithTag("home_open_admin").assertIsDisplayed()
    }

    @Test
    fun regularMember_doesNotSeeAdminButtonAndIsBlockedIfAdminDashboardRendered() {
        var returned = false
        composeRule.setContent {
            var renderDeniedAdmin by remember { mutableStateOf(false) }
            VeloraTheme {
                if (renderDeniedAdmin) {
                    AdminDashboardScreen(
                        isAdmin = false,
                        catalog = emptyList(),
                        genres = emptyList(),
                        users = emptyList(),
                        subscriptionPlans = emptyList(),
                        userSubscriptions = emptyList(),
                        isSavingCatalog = false,
                        isLoadingSeriesSeasons = false,
                        loadedSeriesSeasons = emptyList(),
                        onBack = {
                            returned = true
                            renderDeniedAdmin = false
                        },
                        onSeedCatalog = {},
                        onSyncSubscriptionSchemas = {},
                        onPrepareSeriesEditor = {},
                        onSaveCatalogItem = { _, _ -> },
                        onSaveSeriesWithSeasons = { _, _, _, _, _ -> },
                        onDeleteCatalogItem = { _, _ -> },
                        onAddSeason = { _, _ -> },
                        onAddEpisode = { _, _, _ -> },
                        onSaveGenre = {},
                        onDeleteGenre = {},
                        onToggleUserAdmin = { _, _ -> }
                    )
                } else {
                    ProfileSelectorScreen(
                        profilesState = UiState.Success(listOf(UserProfile(id = "p1", name = "Viewer"))),
                        genres = emptyList(),
                        isAdmin = false,
                        onSelectProfile = { renderDeniedAdmin = true },
                        onOpenAdminDashboard = {},
                        onCreateProfile = { _, _, _, _ -> },
                        onUpdateProfile = { _, _, _, _, _, _, _ -> },
                        onDeleteProfile = {}
                    )
                }
            }
        }

        composeRule.onNodeWithTag("profile_selector_admin_button").assertDoesNotExist()
        composeRule.onNodeWithTag("profile_tile_p1").performClick()
        composeRule.onNodeWithText("Administrator Access Required").assertIsDisplayed()
        composeRule.onNodeWithText("Return").performClick()
        assertTrue(returned)
    }
}
