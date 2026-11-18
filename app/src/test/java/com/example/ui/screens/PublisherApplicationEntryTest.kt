package com.example.ui.screens

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.data.model.AppRole
import com.example.data.model.GovernanceConstants
import com.example.data.model.PublisherApplication
import com.example.data.model.PublisherStatus
import com.example.data.model.SubscriptionCatalogDefaults
import com.example.data.model.UserAccount
import com.example.data.model.UserProfile
import com.example.ui.theme.VeloraTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PublisherApplicationEntryTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun defaultViewerAccount(status: String = "none") = UserAccount(
        userId = "viewer_1",
        email = "viewer@example.com",
        displayName = "Viewer One",
        role = "viewer",
        publisherStatus = status
    )

    @Test
    fun viewer_seesBecomeAPublisherDistributor_andApplyNowOpensExactGoogleForm() {
        composeRule.setContent {
            VeloraTheme {
                SettingsScreen(
                    account = defaultViewerAccount(),
                    activeProfile = UserProfile(id = "p1", name = "Viewer"),
                    effectiveRole = AppRole.VIEWER,
                    isApprovedPublisher = false,
                    myPublisherApplication = null,
                    userSubscription = SubscriptionCatalogDefaults.defaultFreeSubscription("viewer_1"),
                    freePlanFeatures = SubscriptionCatalogDefaults.freePlanFeatures,
                    isAdmin = false,
                    onSwitchProfile = {},
                    onOpenAdminDashboard = {},
                    onOpenPublisherDashboard = {},
                    onSavePreferences = { _, _, _, _, _, _ -> },
                    onSignOut = {}
                )
            }
        }

        composeRule.onNodeWithText("Become a Publisher/Distributor").assertIsDisplayed()
        composeRule.onNodeWithText("Apply Now").assertIsDisplayed().performClick()

        val startedIntent = shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity
        assertNotNull("Expected browser Intent to launch Google Form", startedIntent)
        assertEquals(Intent.ACTION_VIEW, startedIntent.action)
        assertEquals(GovernanceConstants.PUBLISHER_APPLICATION_FORM_URL, startedIntent.dataString)
        composeRule.onNodeWithTag("settings_admin_button").assertDoesNotExist()
    }

    @Test
    fun pendingApplicant_seesApplicationPending_withoutDuplicateApplyButton() {
        val pendingApp = PublisherApplication(
            id = "viewer_1",
            userId = "viewer_1",
            publisherName = "Indie Studio",
            contactEmail = "viewer@example.com",
            status = PublisherStatus.PENDING.key
        )
        composeRule.setContent {
            VeloraTheme {
                SettingsScreen(
                    account = defaultViewerAccount("pending"),
                    activeProfile = UserProfile(id = "p1", name = "Viewer"),
                    effectiveRole = AppRole.VIEWER,
                    isApprovedPublisher = false,
                    myPublisherApplication = pendingApp,
                    userSubscription = SubscriptionCatalogDefaults.defaultFreeSubscription("viewer_1"),
                    freePlanFeatures = SubscriptionCatalogDefaults.freePlanFeatures,
                    isAdmin = false,
                    onSwitchProfile = {},
                    onOpenAdminDashboard = {},
                    onOpenPublisherDashboard = {},
                    onSavePreferences = { _, _, _, _, _, _ -> },
                    onSignOut = {}
                )
            }
        }

        composeRule.onNodeWithTag("settings_publisher_pending_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_apply_publisher_form_button").assertDoesNotExist()
    }

    @Test
    fun approvedPublisher_seesPublisherDashboardInsteadOfApplyNow() {
        var openedDashboard = false
        composeRule.setContent {
            VeloraTheme {
                SettingsScreen(
                    account = UserAccount(
                        userId = "pub_1",
                        email = "pub@studio.com",
                        displayName = "Indie Studio",
                        role = "publisher",
                        publisherStatus = "approved"
                    ),
                    activeProfile = UserProfile(id = "p1", name = "Studio"),
                    effectiveRole = AppRole.PUBLISHER,
                    isApprovedPublisher = true,
                    myPublisherApplication = null,
                    userSubscription = SubscriptionCatalogDefaults.defaultFreeSubscription("pub_1"),
                    freePlanFeatures = SubscriptionCatalogDefaults.freePlanFeatures,
                    isAdmin = false,
                    onSwitchProfile = {},
                    onOpenAdminDashboard = {},
                    onOpenPublisherDashboard = { openedDashboard = true },
                    onSavePreferences = { _, _, _, _, _, _ -> },
                    onSignOut = {}
                )
            }
        }

        composeRule.onNodeWithTag("settings_publisher_dashboard_button").assertIsDisplayed().performClick()
        assertTrue(openedDashboard)
        composeRule.onNodeWithTag("settings_apply_publisher_form_button").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_admin_button").assertDoesNotExist()
    }

    @Test
    fun rejectedApplicant_seesStatusFeedbackAndNextSteps() {
        val rejectedApp = PublisherApplication(
            id = "viewer_1",
            userId = "viewer_1",
            publisherName = "Indie Studio",
            contactEmail = "viewer@example.com",
            status = PublisherStatus.REJECTED.key,
            reviewerNotes = "Please include distribution license documentation."
        )
        composeRule.setContent {
            VeloraTheme {
                SettingsScreen(
                    account = defaultViewerAccount("rejected"),
                    activeProfile = UserProfile(id = "p1", name = "Viewer"),
                    effectiveRole = AppRole.VIEWER,
                    isApprovedPublisher = false,
                    myPublisherApplication = rejectedApp,
                    userSubscription = SubscriptionCatalogDefaults.defaultFreeSubscription("viewer_1"),
                    freePlanFeatures = SubscriptionCatalogDefaults.freePlanFeatures,
                    isAdmin = false,
                    onSwitchProfile = {},
                    onOpenAdminDashboard = {},
                    onOpenPublisherDashboard = {},
                    onSavePreferences = { _, _, _, _, _, _ -> },
                    onSignOut = {}
                )
            }
        }

        composeRule.onNodeWithTag("settings_publisher_rejected_box").assertIsDisplayed()
        composeRule.onNodeWithText("Reviewer Note: Please include distribution license documentation.").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_apply_publisher_form_button").assertIsDisplayed()
    }

    @Test
    fun ownerAndAdmin_retainDashboardAccess() {
        var openedAdmin = false
        var openedPublisher = false
        composeRule.setContent {
            VeloraTheme {
                SettingsScreen(
                    account = UserAccount(
                        userId = "owner_1",
                        email = "michael.tollackson82313@gmail.com",
                        displayName = "Owner",
                        role = "owner"
                    ),
                    activeProfile = UserProfile(id = "p1", name = "Owner"),
                    effectiveRole = AppRole.OWNER,
                    isApprovedPublisher = true,
                    myPublisherApplication = null,
                    userSubscription = SubscriptionCatalogDefaults.defaultFreeSubscription("owner_1"),
                    freePlanFeatures = SubscriptionCatalogDefaults.freePlanFeatures,
                    isAdmin = true,
                    onSwitchProfile = {},
                    onOpenAdminDashboard = { openedAdmin = true },
                    onOpenPublisherDashboard = { openedPublisher = true },
                    onSavePreferences = { _, _, _, _, _, _ -> },
                    onSignOut = {}
                )
            }
        }

        composeRule.onNodeWithTag("settings_publisher_dashboard_button").assertIsDisplayed().performClick()
        assertTrue(openedPublisher)
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag("settings_admin_button"))
        composeRule.onNodeWithTag("settings_admin_button").assertIsDisplayed().performClick()
        assertTrue(openedAdmin)
    }
}
