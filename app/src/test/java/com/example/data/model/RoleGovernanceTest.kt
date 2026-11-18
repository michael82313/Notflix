package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleGovernanceTest {

    @Test
    fun legacyMemberRoleMapsToViewerWithoutDataLoss() {
        assertEquals(AppRole.VIEWER, AppRole.fromKey("member"))
        assertEquals(AppRole.VIEWER, AppRole.fromKey("viewer"))
        assertEquals(AppRole.VIEWER, AppRole.fromKey(null))
        assertEquals(AppRole.VIEWER, AppRole.fromKey(""))
        assertEquals(AppRole.PUBLISHER, AppRole.fromKey("publisher"))
        assertEquals(AppRole.ADMIN, AppRole.fromKey("admin"))
        assertEquals(AppRole.OWNER, AppRole.fromKey("owner"))
    }

    @Test
    fun roleHierarchyResolutionPrioritizesOwnerOverAdminAndPublisher() {
        val ownerAccount = UserAccount(userId = "u1", role = "viewer")
        assertEquals(
            AppRole.OWNER,
            RoleGovernance.resolveEffectiveRole(ownerAccount.role, hasOwnerRecord = true, hasAdminRecord = true)
        )
        assertEquals(
            AppRole.ADMIN,
            RoleGovernance.resolveEffectiveRole(ownerAccount.role, hasOwnerRecord = false, hasAdminRecord = true)
        )
        val publisherAccount = UserAccount(userId = "u2", role = "publisher", publisherStatus = "approved")
        assertEquals(
            AppRole.PUBLISHER,
            RoleGovernance.resolveEffectiveRole(publisherAccount.role, hasOwnerRecord = false, hasAdminRecord = false)
        )
    }

    @Test
    fun ownerProtectionPreventsAdminsFromModifyingOwnerOrManagingAdmins() {
        // Owner can appoint and demote Admins, Publishers, and Viewers
        assertTrue(RoleGovernance.canActorChangeRole(AppRole.OWNER, AppRole.VIEWER, AppRole.ADMIN))
        assertTrue(RoleGovernance.canActorChangeRole(AppRole.OWNER, AppRole.ADMIN, AppRole.VIEWER))
        assertTrue(RoleGovernance.canActorChangeRole(AppRole.OWNER, AppRole.VIEWER, AppRole.PUBLISHER))
        // Nobody can demote Owner or assign another Owner from client role panel
        assertFalse(RoleGovernance.canActorChangeRole(AppRole.OWNER, AppRole.OWNER, AppRole.ADMIN))
        assertFalse(RoleGovernance.canActorChangeRole(AppRole.OWNER, AppRole.ADMIN, AppRole.OWNER))

        // Admin CANNOT modify Owner
        assertFalse(RoleGovernance.canActorChangeRole(AppRole.ADMIN, AppRole.OWNER, AppRole.VIEWER))
        assertFalse(RoleGovernance.canActorChangeRole(AppRole.ADMIN, AppRole.OWNER, AppRole.ADMIN))
        // Admin CANNOT appoint or demote Admins
        assertFalse(RoleGovernance.canActorChangeRole(AppRole.ADMIN, AppRole.VIEWER, AppRole.ADMIN))
        assertFalse(RoleGovernance.canActorChangeRole(AppRole.ADMIN, AppRole.PUBLISHER, AppRole.ADMIN))
        assertFalse(RoleGovernance.canActorChangeRole(AppRole.ADMIN, AppRole.ADMIN, AppRole.VIEWER))
        assertFalse(RoleGovernance.canActorChangeRole(AppRole.ADMIN, AppRole.ADMIN, AppRole.PUBLISHER))
        // Admin CAN manage Viewers and Publishers
        assertTrue(RoleGovernance.canActorChangeRole(AppRole.ADMIN, AppRole.VIEWER, AppRole.PUBLISHER))
        assertTrue(RoleGovernance.canActorChangeRole(AppRole.ADMIN, AppRole.PUBLISHER, AppRole.VIEWER))

        // Publisher & Viewer CANNOT change anyone's role
        assertFalse(RoleGovernance.canActorChangeRole(AppRole.PUBLISHER, AppRole.VIEWER, AppRole.PUBLISHER))
        assertFalse(RoleGovernance.canActorChangeRole(AppRole.VIEWER, AppRole.VIEWER, AppRole.ADMIN))
    }

    @Test
    fun suspensionRulesProtectOwnerAndOtherAdmins() {
        assertFalse(RoleGovernance.canActorSuspendTarget(AppRole.ADMIN, AppRole.OWNER))
        assertFalse(RoleGovernance.canActorSuspendTarget(AppRole.ADMIN, AppRole.ADMIN))
        assertTrue(RoleGovernance.canActorSuspendTarget(AppRole.ADMIN, AppRole.PUBLISHER))
        assertTrue(RoleGovernance.canActorSuspendTarget(AppRole.ADMIN, AppRole.VIEWER))

        assertFalse(RoleGovernance.canActorSuspendTarget(AppRole.OWNER, AppRole.OWNER))
        assertTrue(RoleGovernance.canActorSuspendTarget(AppRole.OWNER, AppRole.ADMIN))
        assertTrue(RoleGovernance.canActorSuspendTarget(AppRole.OWNER, AppRole.PUBLISHER))
    }

    @Test
    fun publisherAccessRequiresApprovedAndUnsuspendedStatus() {
        val approvedPublisher = UserAccount(role = "publisher", publisherStatus = "approved", isSuspended = false)
        val pendingApplicant = UserAccount(role = "viewer", publisherStatus = "pending", isSuspended = false)
        val revokedPublisher = UserAccount(role = "publisher", publisherStatus = "revoked", isSuspended = false)

        assertTrue(RoleGovernance.isApprovedPublisherAccess(AppRole.PUBLISHER, approvedPublisher.parsedPublisherStatus))
        assertFalse(RoleGovernance.isApprovedPublisherAccess(AppRole.VIEWER, pendingApplicant.parsedPublisherStatus))
        assertFalse(RoleGovernance.isApprovedPublisherAccess(AppRole.PUBLISHER, revokedPublisher.parsedPublisherStatus))
        assertTrue(RoleGovernance.isApprovedPublisherAccess(AppRole.ADMIN, pendingApplicant.parsedPublisherStatus))
        assertTrue(RoleGovernance.isApprovedPublisherAccess(AppRole.OWNER, pendingApplicant.parsedPublisherStatus))
    }

    @Test
    fun subscriptionPlanViewerVisibilityFiltersDisabledPrivateAndArchivedPlans() {
        val publicActive = SubscriptionPlan(id = "pro", name = "Pro", isActive = true, isPublic = true, isArchived = false)
        val privateActive = SubscriptionPlan(id = "vip", name = "VIP", isActive = true, isPublic = false, isArchived = false)
        val inactivePublic = SubscriptionPlan(id = "draft", name = "Draft", isActive = false, isPublic = true, isArchived = false)
        val archivedPlan = SubscriptionPlan(id = "legacy", name = "Legacy", isActive = true, isPublic = true, isArchived = true)

        assertTrue(publicActive.isSelectableByViewer)
        assertFalse(privateActive.isSelectableByViewer)
        assertFalse(inactivePublic.isSelectableByViewer)
        assertFalse(archivedPlan.isSelectableByViewer)
    }

    @Test
    fun contentSubmissionInPlaceEditOnlyAllowedBeforeApproval() {
        val pending = ContentSubmission(id = "s1", reviewStatus = ContentReviewStatus.PENDING_REVIEW.key)
        val changesRequested = ContentSubmission(id = "s2", reviewStatus = ContentReviewStatus.CHANGES_REQUESTED.key)
        val approved = ContentSubmission(id = "s3", reviewStatus = ContentReviewStatus.APPROVED.key)
        val rejected = ContentSubmission(id = "s4", reviewStatus = ContentReviewStatus.REJECTED.key)

        assertTrue(pending.canPublisherEditInPlace)
        assertTrue(changesRequested.canPublisherEditInPlace)
        assertFalse(approved.canPublisherEditInPlace)
        assertFalse(rejected.canPublisherEditInPlace)
    }

    @Test
    fun publisherDisclaimerAndGoogleFormConstantsMatchSpecification() {
        assertEquals("https://forms.gle/Bj6NsHnmAxrumCL66", GovernanceConstants.PUBLISHER_APPLICATION_FORM_URL)
        assertTrue(GovernanceConstants.PUBLISHER_APPLICATION_DISCLAIMER.contains("does not guarantee approval"))
        assertTrue(GovernanceConstants.PUBLISHER_APPLICATION_DISCLAIMER.contains("individual content review"))
        assertTrue(GovernanceConstants.PUBLISHER_APPLICATION_DISCLAIMER.contains("does not automatically approve submitted content"))
    }
}
