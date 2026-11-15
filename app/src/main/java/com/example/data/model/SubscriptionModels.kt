package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class SubscriptionPlan(
    @DocumentId val id: String = "",
    val tierCode: String = "free", // free | standard | premium | ultimate
    val name: String = "Free",
    val description: String = "",
    val isPermanentFree: Boolean = false,
    val isActive: Boolean = false,
    val monthlyPriceCents: Int = 0,
    val yearlyPriceCents: Int = 0,
    val maxQuality: String = "Auto",
    val maxSimultaneousStreams: Int = 4,
    val maxProfiles: Int = 5,
    val featuresSummary: List<String> = emptyList(),
    val displayOrder: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toWriteMap(isCreate: Boolean): Map<String, Any> = buildMap {
        put("tierCode", tierCode)
        put("name", name.trim().take(60))
        put("description", description.trim().take(500))
        put("isPermanentFree", isPermanentFree)
        put("isActive", if (isPermanentFree) true else isActive)
        put("monthlyPriceCents", if (isPermanentFree) 0 else monthlyPriceCents.coerceAtLeast(0))
        put("yearlyPriceCents", if (isPermanentFree) 0 else yearlyPriceCents.coerceAtLeast(0))
        put("maxQuality", maxQuality)
        put("maxSimultaneousStreams", maxSimultaneousStreams.coerceIn(1, 20))
        put("maxProfiles", maxProfiles.coerceIn(1, 10))
        put("featuresSummary", featuresSummary.take(20))
        put("displayOrder", displayOrder.coerceIn(0, 100))
        if (isCreate) put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class PlanFeature(
    @DocumentId val id: String = "",
    val featureKey: String = "",
    val label: String = "",
    val description: String = "",
    val enabled: Boolean = true,
    val displayOrder: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toWriteMap(isCreate: Boolean): Map<String, Any> = buildMap {
        put("featureKey", featureKey.trim().take(64))
        put("label", label.trim().take(100))
        put("description", description.trim().take(300))
        put("enabled", enabled)
        put("displayOrder", displayOrder.coerceIn(0, 100))
        if (isCreate) put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

@IgnoreExtraProperties
data class UserSubscription(
    @DocumentId val id: String = "",
    val userId: String = "",
    val planId: String = "free",
    val planName: String = "Free",
    val status: String = "active", // active | paused | expired | canceled
    val isPermanentFree: Boolean = true,
    val assignedBy: String = "system",
    @ServerTimestamp val startsAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun toFreeEnrollmentMap(uid: String): Map<String, Any?> = mapOf(
        "userId" to uid,
        "planId" to "free",
        "planName" to "Free",
        "status" to "active",
        "isPermanentFree" to true,
        "assignedBy" to "system",
        "startsAt" to FieldValue.serverTimestamp(),
        "expiresAt" to null,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

object SubscriptionCatalogDefaults {
    val freePlan = SubscriptionPlan(
        id = "free",
        tierCode = "free",
        name = "Free",
        description = "Permanent free tier with full account access, multi-profile support, full catalog streaming, watchlists, recommendations, and complete video player controls.",
        isPermanentFree = true,
        isActive = true,
        monthlyPriceCents = 0,
        yearlyPriceCents = 0,
        maxQuality = "Auto",
        maxSimultaneousStreams = 4,
        maxProfiles = 5,
        featuresSummary = listOf(
            "Full account & desktop/mobile access",
            "Up to 5 user profiles & Kids mode",
            "All available movies & TV series",
            "Search, watchlists, history & recommendations",
            "Full HD/HLS video player functionality"
        ),
        displayOrder = 0
    )

    val standardBlueprint = SubscriptionPlan(
        id = "standard",
        tierCode = "standard",
        name = "Standard",
        description = "Expansion-ready tier blueprint (inactive — paid subscriptions are not enabled).",
        isPermanentFree = false,
        isActive = false,
        monthlyPriceCents = 0,
        yearlyPriceCents = 0,
        maxQuality = "1080p",
        maxSimultaneousStreams = 2,
        maxProfiles = 5,
        featuresSummary = listOf("Future expansion tier blueprint (Coming Soon)"),
        displayOrder = 1
    )

    val premiumBlueprint = SubscriptionPlan(
        id = "premium",
        tierCode = "premium",
        name = "Premium",
        description = "Expansion-ready tier blueprint (inactive — paid subscriptions are not enabled).",
        isPermanentFree = false,
        isActive = false,
        monthlyPriceCents = 0,
        yearlyPriceCents = 0,
        maxQuality = "4K",
        maxSimultaneousStreams = 4,
        maxProfiles = 5,
        featuresSummary = listOf("Future expansion tier blueprint (Coming Soon)"),
        displayOrder = 2
    )

    val ultimateBlueprint = SubscriptionPlan(
        id = "ultimate",
        tierCode = "ultimate",
        name = "Ultimate",
        description = "Expansion-ready tier blueprint (inactive — paid subscriptions are not enabled).",
        isPermanentFree = false,
        isActive = false,
        monthlyPriceCents = 0,
        yearlyPriceCents = 0,
        maxQuality = "4K",
        maxSimultaneousStreams = 6,
        maxProfiles = 8,
        featuresSummary = listOf("Future expansion tier blueprint (Coming Soon)"),
        displayOrder = 3
    )

    val allBlueprintTiers: List<SubscriptionPlan> = listOf(
        freePlan,
        standardBlueprint,
        premiumBlueprint,
        ultimateBlueprint
    )

    val freePlanFeatures: List<PlanFeature> = listOf(
        PlanFeature(
            id = "full_account_access",
            featureKey = "full_account_access",
            label = "Full Account Access",
            description = "Complete mobile and desktop access with cloud sync.",
            enabled = true,
            displayOrder = 0
        ),
        PlanFeature(
            id = "multi_profiles",
            featureKey = "multi_profiles",
            label = "Multiple User Profiles",
            description = "Create up to 5 separate adult and Kids profiles.",
            enabled = true,
            displayOrder = 1
        ),
        PlanFeature(
            id = "full_catalog",
            featureKey = "full_catalog",
            label = "Movies & TV Shows",
            description = "Unrestricted access to all available titles and seasons.",
            enabled = true,
            displayOrder = 2
        ),
        PlanFeature(
            id = "discovery_tools",
            featureKey = "discovery_tools",
            label = "Search, Watchlist & History",
            description = "Dynamic search, personal My List, Continue Watching, and recommendations.",
            enabled = true,
            displayOrder = 3
        ),
        PlanFeature(
            id = "full_player",
            featureKey = "full_player",
            label = "Full Video Player",
            description = "MP4 & HLS streaming with speed, captions, audio tracks, and resume.",
            enabled = true,
            displayOrder = 4
        )
    )

    fun defaultFreeSubscription(uid: String): UserSubscription = UserSubscription(
        id = uid,
        userId = uid,
        planId = "free",
        planName = "Free",
        status = "active",
        isPermanentFree = true,
        assignedBy = "system",
        expiresAt = null
    )
}
