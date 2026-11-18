package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppRole
import com.example.data.model.GovernanceConstants
import com.example.data.model.PlanFeature
import com.example.data.model.PlatformSettings
import com.example.data.model.PublisherApplication
import com.example.data.model.PublisherStatus
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UserAccount
import com.example.data.model.UserProfile
import com.example.data.model.UserSubscription
import com.example.ui.components.ProfileAvatar
import com.example.ui.theme.VeloraBackground
import com.example.ui.theme.VeloraCrimson
import com.example.ui.theme.VeloraError
import com.example.ui.theme.VeloraGold
import com.example.ui.theme.VeloraKidsCyan
import com.example.ui.theme.VeloraMatchGreen
import com.example.ui.theme.VeloraOnBackground
import com.example.ui.theme.VeloraOnSurfaceMuted
import com.example.ui.theme.VeloraOutline
import com.example.ui.theme.VeloraSurfaceElevated

@Composable
fun SettingsScreen(
    account: UserAccount?,
    activeProfile: UserProfile?,
    effectiveRole: AppRole = AppRole.VIEWER,
    isApprovedPublisher: Boolean = false,
    myPublisherApplication: PublisherApplication? = null,
    platformSettings: PlatformSettings = PlatformSettings(),
    userSubscription: UserSubscription,
    availablePublicPlans: List<SubscriptionPlan> = emptyList(),
    freePlanFeatures: List<PlanFeature>,
    isAdmin: Boolean,
    onSwitchProfile: () -> Unit,
    onOpenAdminDashboard: () -> Unit,
    onOpenPublisherDashboard: () -> Unit = {},
    onSubmitPublisherApplicationTracker: (
        publisherName: String,
        contactEmail: String,
        organizationDescription: String,
        intendedContentDetails: String,
        onComplete: () -> Unit
    ) -> Unit = { _, _, _, _, _ -> },
    onSelectPublicPlan: (SubscriptionPlan) -> Unit = {},
    onSavePreferences: (
        displayName: String,
        language: String,
        autoplayNext: Boolean,
        defaultQuality: String,
        subtitlesEnabled: Boolean,
        subtitleSize: String
    ) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var displayName by remember(account?.displayName) { mutableStateOf(account?.displayName ?: "Notflix Viewer") }
    var language by remember(account?.languagePreference) { mutableStateOf(account?.languagePreference ?: "English") }
    var autoplayNext by remember(account?.autoplayNext) { mutableStateOf(account?.autoplayNext ?: true) }
    var quality by remember(account?.defaultQuality) { mutableStateOf(account?.defaultQuality ?: "Auto") }
    var subtitlesEnabled by remember(account?.subtitlesEnabled) { mutableStateOf(account?.subtitlesEnabled ?: true) }
    var subtitleSize by remember(account?.subtitleSize) { mutableStateOf(account?.subtitleSize ?: "Medium") }

    var showLanguageMenu by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var showSubSizeMenu by remember { mutableStateOf(false) }
    var showApplicationTrackerDialog by remember { mutableStateOf(false) }
    var browserFallbackMessage by remember { mutableStateOf<String?>(null) }

    val openPublisherForm: () -> Unit = {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GovernanceConstants.PUBLISHER_APPLICATION_FORM_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val opened = runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
        if (!opened) {
            browserFallbackMessage = "No browser app could open the form automatically. Copy the link below and open it in any web browser."
        }
    }

    val publicPlans = remember(availablePublicPlans) {
        availablePublicPlans.filter { it.isSelectableByViewer }
    }

    val resolvedPublisherStatus = remember(myPublisherApplication, account, isApprovedPublisher, effectiveRole) {
        when {
            effectiveRole == AppRole.PUBLISHER && isApprovedPublisher -> PublisherStatus.APPROVED
            myPublisherApplication != null -> myPublisherApplication.parsedStatus
            else -> account?.parsedPublisherStatus ?: PublisherStatus.NONE
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VeloraBackground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Account Details",
                    style = MaterialTheme.typography.headlineLarge,
                    color = VeloraOnBackground
                )
                val roleColor = when (effectiveRole) {
                    AppRole.OWNER -> VeloraGold
                    AppRole.ADMIN -> VeloraCrimson
                    AppRole.PUBLISHER -> VeloraKidsCyan
                    AppRole.VIEWER -> VeloraOnSurfaceMuted
                }
                Surface(
                    color = roleColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, roleColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = effectiveRole.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = roleColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Active Profile & Switcher
        item {
            Surface(
                color = VeloraSurfaceElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, VeloraOutline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileAvatar(
                            avatarKey = activeProfile?.avatarKey ?: "crimson",
                            name = activeProfile?.name ?: "V",
                            isKids = activeProfile?.isKids == true,
                            size = 52.dp
                        )
                        Column {
                            Text(
                                text = activeProfile?.name ?: "Main Profile",
                                style = MaterialTheme.typography.titleLarge,
                                color = VeloraOnBackground
                            )
                            Text(
                                text = if (activeProfile?.isKids == true) "Kids & Parental Safe Mode Active" else "Standard Profile • All Ratings",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (activeProfile?.isKids == true) VeloraKidsCyan else VeloraOnSurfaceMuted
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onSwitchProfile,
                        border = BorderStroke(1.dp, VeloraOutline),
                        modifier = Modifier.testTag("settings_switch_profile_button")
                    ) {
                        Icon(Icons.Filled.People, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Switch", color = VeloraOnBackground)
                    }
                }
            }
        }

        // Become a Publisher/Distributor Section — prominently placed under Account Details
        item {
            val isPendingApplication = resolvedPublisherStatus == PublisherStatus.PENDING ||
                resolvedPublisherStatus == PublisherStatus.UNDER_REVIEW
            val isRejectedApplication = resolvedPublisherStatus == PublisherStatus.REJECTED

            Surface(
                color = VeloraSurfaceElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, VeloraKidsCyan.copy(alpha = 0.45f)),
                modifier = Modifier.testTag("settings_publisher_section")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Publish, contentDescription = null, tint = VeloraKidsCyan, modifier = Modifier.size(26.dp))
                            Column {
                                Text(
                                    text = if (isApprovedPublisher && !isAdmin) {
                                        "Publisher/Distributor Portal"
                                    } else {
                                        "Become a Publisher/Distributor"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = VeloraOnBackground
                                )
                                Text(
                                    text = when {
                                        isApprovedPublisher && !isAdmin -> "Approved Publisher/Distributor"
                                        isPendingApplication -> "Application Pending"
                                        isRejectedApplication -> "Application Not Approved"
                                        isAdmin -> "Role: ${effectiveRole.label} • Application Entry Preview"
                                        else -> "Apply to publish movies & TV shows"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when {
                                        isApprovedPublisher -> VeloraMatchGreen
                                        isRejectedApplication || resolvedPublisherStatus == PublisherStatus.REVOKED || resolvedPublisherStatus == PublisherStatus.SUSPENDED -> VeloraError
                                        isPendingApplication -> VeloraGold
                                        else -> VeloraOnSurfaceMuted
                                    }
                                )
                            }
                        }
                    }

                    if (isApprovedPublisher) {
                        Text(
                            text = "Submit movies and television shows to Notflix for review and track your submission status.",
                            style = MaterialTheme.typography.bodySmall,
                            color = VeloraOnSurfaceMuted
                        )
                        Button(
                            onClick = onOpenPublisherDashboard,
                            colors = ButtonDefaults.buttonColors(containerColor = VeloraKidsCyan),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_publisher_dashboard_button")
                        ) {
                            Text("Open Publisher Dashboard", color = VeloraBackground, fontWeight = FontWeight.Bold)
                        }
                        if (isAdmin) {
                            OutlinedButton(
                                onClick = openPublisherForm,
                                border = BorderStroke(1.dp, VeloraOutline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_preview_publisher_form_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp), tint = VeloraOnSurfaceMuted)
                                Spacer(Modifier.width(6.dp))
                                Text("Preview External Application Form", color = VeloraOnSurfaceMuted)
                            }
                        }
                    } else if (isPendingApplication) {
                        Surface(
                            color = VeloraGold.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, VeloraGold.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_publisher_pending_badge")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Application Pending",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = VeloraGold
                                )
                                Text(
                                    text = "Your Publisher/Distributor application is currently under review. Please wait for review rather than submitting duplicate applications.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VeloraOnSurfaceMuted
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Apply to submit movies and television shows to Notflix for review. ${GovernanceConstants.PUBLISHER_APPLICATION_DISCLAIMER}",
                            style = MaterialTheme.typography.bodySmall,
                            color = VeloraOnSurfaceMuted
                        )

                        if (isRejectedApplication) {
                            Surface(
                                color = VeloraError.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, VeloraError.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_publisher_rejected_box")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Previous Application Not Approved",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = VeloraError
                                    )
                                    val note = myPublisherApplication?.reviewerNotes?.takeIf { it.isNotBlank() }
                                    Text(
                                        text = note?.let { "Reviewer Note: $it" }
                                            ?: "Next step: Review your distribution eligibility and submit an updated application via the form below when ready.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = VeloraOnSurfaceMuted
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = openPublisherForm,
                                colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_apply_publisher_form_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Apply Now")
                            }

                            OutlinedButton(
                                onClick = { showApplicationTrackerDialog = true },
                                border = BorderStroke(1.dp, VeloraKidsCyan.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_track_publisher_app_button")
                            ) {
                                Text("Track Application", color = VeloraKidsCyan)
                            }
                        }
                    }
                }
            }
        }

        // Subscription & Available Public Plans
        item {
            Surface(
                color = VeloraMatchGreen.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, VeloraMatchGreen.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (platformSettings.freeAccessEnabled) {
                        Surface(
                            color = VeloraMatchGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "FREE ACCESS MODE ACTIVE • ${platformSettings.freeAccessNotice}",
                                style = MaterialTheme.typography.labelSmall,
                                color = VeloraMatchGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Verified, contentDescription = null, tint = VeloraMatchGreen)
                        Column {
                            Text(
                                text = "${userSubscription.planName} Plan • ${userSubscription.status.replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = VeloraOnBackground
                            )
                            Text(
                                text = if (userSubscription.isPermanentFree) {
                                    "Permanent Free Tier • Never Expires • Full Access Included"
                                } else {
                                    "Managed Subscription • Status: ${userSubscription.status}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = VeloraMatchGreen
                            )
                        }
                    }

                    freePlanFeatures.filter { it.enabled }.forEach { feature ->
                        Text(
                            text = "• ${feature.label}: ${feature.description}",
                            style = MaterialTheme.typography.bodySmall,
                            color = VeloraOnSurfaceMuted
                        )
                    }

                    if (publicPlans.size > 1) {
                        Spacer(Modifier.height(4.dp))
                        Text("Available Public Plans", style = MaterialTheme.typography.labelLarge, color = VeloraOnBackground)
                        publicPlans.forEach { plan ->
                            val isCurrent = userSubscription.planId == plan.id
                            Surface(
                                color = VeloraBackground,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isCurrent) VeloraMatchGreen else VeloraOutline)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("${plan.name} • ${plan.formattedPriceLabel()}", style = MaterialTheme.typography.titleSmall, color = VeloraOnBackground)
                                        Text("${plan.maxQuality} • Up to ${plan.maxProfiles} profiles", style = MaterialTheme.typography.bodySmall, color = VeloraOnSurfaceMuted)
                                    }
                                    if (isCurrent) {
                                        Text("Current", style = MaterialTheme.typography.labelMedium, color = VeloraMatchGreen, fontWeight = FontWeight.Bold)
                                    } else {
                                        OutlinedButton(onClick = { onSelectPublicPlan(plan) }) {
                                            Text("Select", color = VeloraOnBackground)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Admin Dashboard Entry (Owner & Admins only)
        if (isAdmin) {
            item {
                Surface(
                    color = VeloraCrimson.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, VeloraCrimson.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenAdminDashboard)
                        .testTag("settings_admin_button")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = VeloraGold, modifier = Modifier.size(28.dp))
                            Column {
                                Text(
                                    text = if (effectiveRole.isOwner) "Owner & Admin Dashboard" else "Admin Dashboard",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = VeloraOnBackground
                                )
                                Text(
                                    text = "Content review, publisher approvals, users, plans, settings & audit log",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VeloraOnSurfaceMuted
                                )
                            }
                        }
                        Text("Open", color = VeloraCrimson, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Playback, Language & Subtitle Settings
        item {
            Surface(
                color = VeloraSurfaceElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, VeloraOutline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Account & Playback Preferences", style = MaterialTheme.typography.titleLarge, color = VeloraOnBackground)

                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Account Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val signedInIdentifier = account?.email?.takeIf { it.isNotBlank() }
                        ?: account?.phoneNumber?.takeIf { it.isNotBlank() }
                    signedInIdentifier?.let { identifier ->
                        Text("Signed in as $identifier", style = MaterialTheme.typography.bodySmall, color = VeloraOnSurfaceMuted)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Autoplay Next Episode", style = MaterialTheme.typography.titleSmall, color = VeloraOnBackground)
                            Text("Automatically start the next episode when credits roll", style = MaterialTheme.typography.bodySmall, color = VeloraOnSurfaceMuted)
                        }
                        Switch(checked = autoplayNext, onCheckedChange = { autoplayNext = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Subtitles Enabled by Default", style = MaterialTheme.typography.titleSmall, color = VeloraOnBackground)
                        Switch(checked = subtitlesEnabled, onCheckedChange = { subtitlesEnabled = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Audio & UI Language", style = MaterialTheme.typography.titleSmall, color = VeloraOnBackground)
                        Column {
                            OutlinedButton(onClick = { showLanguageMenu = true }) {
                                Text(language, color = VeloraOnBackground)
                            }
                            DropdownMenu(expanded = showLanguageMenu, onDismissRequest = { showLanguageMenu = false }, containerColor = VeloraSurfaceElevated) {
                                listOf("English", "Spanish", "French", "German", "Japanese").forEach { lang ->
                                    DropdownMenuItem(text = { Text(lang, color = VeloraOnBackground) }, onClick = {
                                        language = lang
                                        showLanguageMenu = false
                                    })
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Default Streaming Quality", style = MaterialTheme.typography.titleSmall, color = VeloraOnBackground)
                        Column {
                            OutlinedButton(onClick = { showQualityMenu = true }) {
                                Text(quality, color = VeloraOnBackground)
                            }
                            DropdownMenu(expanded = showQualityMenu, onDismissRequest = { showQualityMenu = false }, containerColor = VeloraSurfaceElevated) {
                                listOf("Auto", "1080p", "720p", "480p").forEach { q ->
                                    DropdownMenuItem(text = { Text(q, color = VeloraOnBackground) }, onClick = {
                                        quality = q
                                        showQualityMenu = false
                                    })
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Subtitle Size", style = MaterialTheme.typography.titleSmall, color = VeloraOnBackground)
                        Column {
                            OutlinedButton(onClick = { showSubSizeMenu = true }) {
                                Text(subtitleSize, color = VeloraOnBackground)
                            }
                            DropdownMenu(expanded = showSubSizeMenu, onDismissRequest = { showSubSizeMenu = false }, containerColor = VeloraSurfaceElevated) {
                                listOf("Small", "Medium", "Large").forEach { sz ->
                                    DropdownMenuItem(text = { Text(sz, color = VeloraOnBackground) }, onClick = {
                                        subtitleSize = sz
                                        showSubSizeMenu = false
                                    })
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            onSavePreferences(
                                displayName,
                                language,
                                autoplayNext,
                                quality,
                                subtitlesEnabled,
                                subtitleSize
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_preferences_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson)
                    ) {
                        Text("Save Preferences")
                    }
                }
            }
        }

        // Device Session Info
        item {
            Surface(
                color = VeloraSurfaceElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, VeloraOutline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Devices, contentDescription = null, tint = VeloraOnSurfaceMuted)
                    Column {
                        Text("Current Device Session", style = MaterialTheme.typography.titleSmall, color = VeloraOnBackground)
                        Text(
                            text = "${Build.MANUFACTURER} ${Build.MODEL} • Android ${Build.VERSION.RELEASE} • Cloud Sync Active",
                            style = MaterialTheme.typography.bodySmall,
                            color = VeloraOnSurfaceMuted
                        )
                    }
                }
            }
        }

        // Sign Out
        item {
            OutlinedButton(
                onClick = onSignOut,
                border = BorderStroke(1.dp, VeloraError.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = VeloraError)
                Spacer(Modifier.width(8.dp))
                Text("Sign Out", color = VeloraError, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showApplicationTrackerDialog) {
        var pubName by remember { mutableStateOf(myPublisherApplication?.publisherName ?: account?.displayName.orEmpty()) }
        var contactEmail by remember { mutableStateOf(myPublisherApplication?.contactEmail ?: account?.email.orEmpty()) }
        var orgDesc by remember { mutableStateOf(myPublisherApplication?.organizationDescription.orEmpty()) }
        var intendedContent by remember { mutableStateOf(myPublisherApplication?.intendedContentDetails.orEmpty()) }

        AlertDialog(
            onDismissRequest = { showApplicationTrackerDialog = false },
            containerColor = VeloraSurfaceElevated,
            title = { Text("Track Publisher Application", color = VeloraOnBackground) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = GovernanceConstants.PUBLISHER_APPLICATION_DISCLAIMER,
                        style = MaterialTheme.typography.bodySmall,
                        color = VeloraGold
                    )
                    OutlinedTextField(value = pubName, onValueChange = { pubName = it }, label = { Text("Publisher / Studio Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = contactEmail, onValueChange = { contactEmail = it }, label = { Text("Private Contact Email *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = orgDesc, onValueChange = { orgDesc = it }, label = { Text("Organization / Creator Overview") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = intendedContent, onValueChange = { intendedContent = it }, label = { Text("Intended Movies / TV Shows") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    enabled = pubName.isNotBlank() && contactEmail.isNotBlank(),
                    onClick = {
                        onSubmitPublisherApplicationTracker(pubName, contactEmail, orgDesc, intendedContent) {
                            showApplicationTrackerDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson)
                ) { Text("Record as Pending") }
            },
            dismissButton = {
                TextButton(onClick = { showApplicationTrackerDialog = false }) { Text("Cancel", color = VeloraOnSurfaceMuted) }
            }
        )
    }

    browserFallbackMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { browserFallbackMessage = null },
            containerColor = VeloraSurfaceElevated,
            title = { Text("Open Publisher Application Form", color = VeloraOnBackground) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(msg, style = MaterialTheme.typography.bodySmall, color = VeloraOnSurfaceMuted)
                    Text(
                        text = GovernanceConstants.PUBLISHER_APPLICATION_FORM_URL,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = VeloraKidsCyan
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(GovernanceConstants.PUBLISHER_APPLICATION_FORM_URL))
                        browserFallbackMessage = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson)
                ) { Text("Copy Form Link") }
            },
            dismissButton = {
                TextButton(onClick = { browserFallbackMessage = null }) { Text("Close", color = VeloraOnSurfaceMuted) }
            }
        )
    }
}
