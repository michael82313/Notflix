package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppRole
import com.example.data.model.AuditLogEntry
import com.example.data.model.PlatformSettings
import com.example.data.model.PublisherApplication
import com.example.data.model.PublisherStatus
import com.example.data.model.RoleGovernance
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UserAccount
import com.example.data.model.UserSubscription
import com.example.ui.theme.VeloraBackground
import com.example.ui.theme.VeloraCoral
import com.example.ui.theme.VeloraCrimson
import com.example.ui.theme.VeloraError
import com.example.ui.theme.VeloraGold
import com.example.ui.theme.VeloraKidsCyan
import com.example.ui.theme.VeloraMatchGreen
import com.example.ui.theme.VeloraOnBackground
import com.example.ui.theme.VeloraOnSurfaceMuted
import com.example.ui.theme.VeloraOutline
import com.example.ui.theme.VeloraSurfaceElevated
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminUserManagementTab(
    actorRole: AppRole,
    users: List<UserAccount>,
    onAssignRole: (UserAccount, AppRole, PublisherStatus?) -> Unit,
    onToggleSuspended: (UserAccount, Boolean) -> Unit
) {
    var pendingRoleChange by remember { mutableStateOf<Triple<UserAccount, AppRole, PublisherStatus?>?>(null) }

    LazyColumn(
        modifier = Modifier.testTag("admin_users_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(
                color = if (actorRole.isOwner) VeloraGold.copy(alpha = 0.14f) else VeloraSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (actorRole.isOwner) VeloraGold.copy(alpha = 0.5f) else VeloraOutline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Security, contentDescription = null, tint = if (actorRole.isOwner) VeloraGold else VeloraKidsCyan)
                    Column {
                        Text(
                            text = if (actorRole.isOwner) "Owner Authority Active" else "Administrator Role Scope",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = VeloraOnBackground
                        )
                        Text(
                            text = if (actorRole.isOwner) {
                                "As Platform Owner, you can appoint/remove Admins and manage Viewer/Publisher roles. Your Owner record is permanently protected."
                            } else {
                                "Admins can manage Viewer and Publisher accounts. Owner and Admin role assignments are restricted exclusively to the Platform Owner."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = VeloraOnSurfaceMuted
                        )
                    }
                }
            }
        }

        itemsIndexed(users, key = { idx, u -> "${u.id.ifBlank { u.userId }.ifBlank { "u" }}_$idx" }) { _, user ->
            val targetRole = user.parsedRole
            val isTargetOwner = targetRole == AppRole.OWNER
            val isTargetAdmin = targetRole == AppRole.ADMIN
            val canManageOrdinary = !isTargetOwner && (actorRole.isOwner || !isTargetAdmin)
            val canAppointOrRemoveAdmin = actorRole.isOwner && !isTargetOwner

            val roleColor = when (targetRole) {
                AppRole.OWNER -> VeloraGold
                AppRole.ADMIN -> VeloraCrimson
                AppRole.PUBLISHER -> VeloraKidsCyan
                AppRole.VIEWER -> VeloraOnSurfaceMuted
            }

            Surface(
                color = VeloraSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isTargetOwner) VeloraGold.copy(alpha = 0.6f) else VeloraOutline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = user.displayName.ifBlank { user.email.ifBlank { user.userId.ifBlank { "Notflix User" } } },
                                style = MaterialTheme.typography.titleMedium,
                                color = VeloraOnBackground
                            )
                            Text(
                                text = "${user.email.ifBlank { user.phoneNumber ?: user.userId }} • Publisher: ${user.parsedPublisherStatus.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = VeloraOnSurfaceMuted
                            )
                        }
                        Surface(
                            color = roleColor.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isTargetOwner) "OWNER (PROTECTED)" else targetRole.label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = roleColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (canManageOrdinary) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            if (targetRole == AppRole.VIEWER) {
                                OutlinedButton(
                                    onClick = { pendingRoleChange = Triple(user, AppRole.PUBLISHER, PublisherStatus.APPROVED) },
                                    border = BorderStroke(1.dp, VeloraKidsCyan.copy(alpha = 0.6f)),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Make Publisher", color = VeloraKidsCyan) }
                            } else if (targetRole == AppRole.PUBLISHER) {
                                OutlinedButton(
                                    onClick = { pendingRoleChange = Triple(user, AppRole.VIEWER, PublisherStatus.REVOKED) },
                                    border = BorderStroke(1.dp, VeloraOutline),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Revoke Publisher", color = VeloraOnBackground) }
                            }

                            if (canAppointOrRemoveAdmin) {
                                if (isTargetAdmin) {
                                    OutlinedButton(
                                        onClick = { pendingRoleChange = Triple(user, AppRole.VIEWER, null) },
                                        border = BorderStroke(1.dp, VeloraError.copy(alpha = 0.7f)),
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Remove Admin", color = VeloraError) }
                                } else {
                                    Button(
                                        onClick = { pendingRoleChange = Triple(user, AppRole.ADMIN, null) },
                                        colors = ButtonDefaults.buttonColors(containerColor = VeloraGold),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("owner_appoint_admin_${user.userId}")
                                    ) { Text("Appoint Admin", color = VeloraBackground, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }

                        if (RoleGovernance.canActorSuspendTarget(actorRole, targetRole)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (user.isSuspended) "Account Suspended" else "Account Active",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (user.isSuspended) VeloraError else VeloraMatchGreen
                                )
                                TextButton(onClick = { onToggleSuspended(user, !user.isSuspended) }) {
                                    Text(
                                        if (user.isSuspended) "Restore Account" else "Suspend Account",
                                        color = if (user.isSuspended) VeloraMatchGreen else VeloraError
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingRoleChange?.let { (target, newRole, pubStatus) ->
        AlertDialog(
            onDismissRequest = { pendingRoleChange = null },
            containerColor = VeloraSurfaceElevated,
            title = { Text("Confirm Role Update", color = VeloraOnBackground) },
            text = {
                Text(
                    "Change ${target.displayName.ifBlank { target.email }} from ${target.parsedRole.label} to ${newRole.label}? This action will be permanently recorded in the audit log.",
                    color = VeloraOnSurfaceMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingRoleChange = null
                        onAssignRole(target, newRole, pubStatus)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson)
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRoleChange = null }) { Text("Cancel", color = VeloraOnSurfaceMuted) }
            }
        )
    }
}

@Composable
fun AdminPublisherManagementTab(
    applications: List<PublisherApplication>,
    users: List<UserAccount>,
    onReviewApplication: (PublisherApplication, PublisherStatus, String) -> Unit,
    onUpdatePublisherStatus: (UserAccount, PublisherStatus) -> Unit
) {
    var reviewingApp by remember { mutableStateOf<PublisherApplication?>(null) }
    val publishers = remember(users) {
        users.filter { it.parsedRole == AppRole.PUBLISHER || it.parsedPublisherStatus != PublisherStatus.NONE }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Publisher/Distributor Applications (${applications.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = VeloraOnBackground
            )
        }

        if (applications.isEmpty()) {
            item {
                Surface(
                    color = VeloraSurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, VeloraOutline)
                ) {
                    Text(
                        "No publisher applications recorded yet. Viewers apply via Settings -> Publisher/Distributor (Content Publisher).",
                        style = MaterialTheme.typography.bodySmall,
                        color = VeloraOnSurfaceMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            itemsIndexed(applications, key = { idx, a -> "${a.id.ifBlank { a.userId }}_$idx" }) { _, app ->
                val color = when (app.parsedStatus) {
                    PublisherStatus.APPROVED -> VeloraMatchGreen
                    PublisherStatus.REJECTED -> VeloraError
                    PublisherStatus.UNDER_REVIEW -> VeloraKidsCyan
                    else -> VeloraGold
                }
                Surface(
                    color = VeloraSurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, VeloraOutline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(app.publisherName.ifBlank { "Applicant ${app.userId.take(8)}" }, style = MaterialTheme.typography.titleMedium, color = VeloraOnBackground)
                                Text("Private Contact: ${app.contactEmail.ifBlank { "(Provided in Google Form)" }}", style = MaterialTheme.typography.bodySmall, color = VeloraOnSurfaceMuted)
                            }
                            Surface(color = color.copy(alpha = 0.16f), shape = RoundedCornerShape(8.dp)) {
                                Text(app.parsedStatus.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = color, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                        if (app.organizationDescription.isNotBlank()) {
                            Text(app.organizationDescription, style = MaterialTheme.typography.bodySmall, color = VeloraOnSurfaceMuted)
                        }
                        if (app.intendedContentDetails.isNotBlank()) {
                            Text("Content Plan: ${app.intendedContentDetails}", style = MaterialTheme.typography.bodySmall, color = VeloraOnSurfaceMuted)
                        }
                        Button(
                            onClick = { reviewingApp = app },
                            colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Review Application") }
                    }
                }
            }
        }

        item {
            Text(
                "Active & Managed Publishers (${publishers.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = VeloraOnBackground
            )
        }

        itemsIndexed(publishers, key = { idx, p -> "pub_${p.userId}_$idx" }) { _, pub ->
            Surface(
                color = VeloraSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, VeloraOutline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(pub.publisherName ?: pub.displayName.ifBlank { pub.email }, style = MaterialTheme.typography.titleMedium, color = VeloraOnBackground)
                            Text("Role: ${pub.parsedRole.label} • Status: ${pub.parsedPublisherStatus.label}", style = MaterialTheme.typography.bodySmall, color = VeloraOnSurfaceMuted)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onUpdatePublisherStatus(pub, PublisherStatus.APPROVED) }) {
                            Text("Approve", color = VeloraMatchGreen)
                        }
                        OutlinedButton(onClick = { onUpdatePublisherStatus(pub, PublisherStatus.SUSPENDED) }) {
                            Text("Suspend", color = VeloraGold)
                        }
                        OutlinedButton(onClick = { onUpdatePublisherStatus(pub, PublisherStatus.REVOKED) }) {
                            Text("Revoke", color = VeloraError)
                        }
                    }
                }
            }
        }
    }

    reviewingApp?.let { app ->
        var notes by remember(app.id) { mutableStateOf(app.reviewerNotes) }
        AlertDialog(
            onDismissRequest = { reviewingApp = null },
            containerColor = VeloraSurfaceElevated,
            title = { Text("Review Publisher Application", color = VeloraOnBackground) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "${app.publisherName.ifBlank { app.userId }} • ${app.contactEmail}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VeloraGold
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Reviewer Notes (Private)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onReviewApplication(app, PublisherStatus.APPROVED, notes)
                            reviewingApp = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VeloraMatchGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Approve as Publisher/Distributor") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                onReviewApplication(app, PublisherStatus.UNDER_REVIEW, notes)
                                reviewingApp = null
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Under Review", color = VeloraKidsCyan) }
                        OutlinedButton(
                            onClick = {
                                onReviewApplication(app, PublisherStatus.REJECTED, notes)
                                reviewingApp = null
                            },
                            border = BorderStroke(1.dp, VeloraError),
                            modifier = Modifier.weight(1f)
                        ) { Text("Reject", color = VeloraError) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { reviewingApp = null }) { Text("Close", color = VeloraOnSurfaceMuted) }
            }
        )
    }
}

@Composable
fun AdminUserSubscriptionsTab(
    users: List<UserAccount>,
    plans: List<SubscriptionPlan>,
    userSubscriptions: List<UserSubscription>,
    onAssignPlan: (userId: String, plan: SubscriptionPlan, status: String) -> Unit,
    onUpdateStatus: (userId: String, newStatus: String) -> Unit
) {
    val subByUser = remember(userSubscriptions) { userSubscriptions.associateBy { it.userId.ifBlank { it.id } } }
    val activePlans = remember(plans) { plans.filter { !it.isArchived } }
    var editingUser by remember { mutableStateOf<UserAccount?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "Manual User Subscription Management (${users.size} Users)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = VeloraOnBackground
            )
        }
        itemsIndexed(users, key = { idx, u -> "subuser_${u.userId}_$idx" }) { _, user ->
            val sub = subByUser[user.userId.ifBlank { user.id }]
            Surface(
                color = VeloraSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, VeloraOutline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(user.displayName.ifBlank { user.email.ifBlank { user.userId } }, style = MaterialTheme.typography.titleMedium, color = VeloraOnBackground)
                        Text(
                            text = "Plan: ${sub?.planName ?: "Free"} • Status: ${(sub?.status ?: "active").uppercase()} • By: ${sub?.assignedBy ?: "system"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = VeloraMatchGreen
                        )
                    }
                    OutlinedButton(onClick = { editingUser = user }) {
                        Text("Manage", color = VeloraOnBackground)
                    }
                }
            }
        }
    }

    editingUser?.let { target ->
        val uid = target.userId.ifBlank { target.id }
        val currentSub = subByUser[uid]
        var selectedPlan by remember(uid) {
            mutableStateOf(activePlans.firstOrNull { it.id == currentSub?.planId } ?: activePlans.firstOrNull())
        }
        var selectedStatus by remember(uid) { mutableStateOf(currentSub?.status ?: "active") }
        var showPlanMenu by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { editingUser = null },
            containerColor = VeloraSurfaceElevated,
            title = { Text("Manage Subscription", color = VeloraOnBackground) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(target.displayName.ifBlank { target.email }, style = MaterialTheme.typography.bodyMedium, color = VeloraGold)
                    Column {
                        OutlinedButton(onClick = { showPlanMenu = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Plan: ${selectedPlan?.name ?: "Select Plan"} (${selectedPlan?.formattedPriceLabel() ?: ""})", color = VeloraOnBackground)
                        }
                        DropdownMenu(expanded = showPlanMenu, onDismissRequest = { showPlanMenu = false }, containerColor = VeloraSurfaceElevated) {
                            activePlans.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text("${p.name} • ${p.formattedPriceLabel()}", color = VeloraOnBackground) },
                                    onClick = {
                                        selectedPlan = p
                                        showPlanMenu = false
                                    }
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("active", "paused", "canceled").forEach { st ->
                            FilterChip(
                                selected = selectedStatus == st,
                                onClick = { selectedStatus = st },
                                label = { Text(st.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = selectedPlan
                        if (p != null) {
                            onAssignPlan(uid, p, selectedStatus)
                        } else {
                            onUpdateStatus(uid, selectedStatus)
                        }
                        editingUser = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson)
                ) { Text("Save Assignment") }
            },
            dismissButton = {
                TextButton(onClick = { editingUser = null }) { Text("Cancel", color = VeloraOnSurfaceMuted) }
            }
        )
    }
}

@Composable
fun AdminPlanPricingTab(
    plans: List<SubscriptionPlan>,
    onSyncSchemas: () -> Unit,
    onSavePlan: (SubscriptionPlan, () -> Unit) -> Unit,
    onArchivePlan: (SubscriptionPlan, Boolean) -> Unit
) {
    var editingPlan by remember { mutableStateOf<SubscriptionPlan?>(null) }
    var creatingNew by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { creatingNew = true },
                    colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_create_plan_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Create Plan")
                }
                OutlinedButton(
                    onClick = onSyncSchemas,
                    border = BorderStroke(1.dp, VeloraMatchGreen.copy(alpha = 0.6f))
                ) { Text("Sync Defaults", color = VeloraMatchGreen) }
            }
        }

        itemsIndexed(plans, key = { idx, p -> "plan_${p.id}_$idx" }) { _, plan ->
            Surface(
                color = VeloraSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (plan.isPermanentFree) VeloraMatchGreen.copy(alpha = 0.5f) else VeloraOutline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("${plan.name} • ${plan.formattedPriceLabel()}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = VeloraOnBackground)
                            Text(
                                text = buildString {
                                    append(if (plan.isArchived) "ARCHIVED" else if (plan.isActive) "ENABLED" else "DISABLED")
                                    append(" • ")
                                    append(if (plan.isPublic && !plan.isArchived) "PUBLIC" else "PRIVATE")
                                    append(" • ${plan.maxQuality} • ${plan.maxProfiles} Profiles")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (plan.isSelectableByViewer) VeloraMatchGreen else VeloraGold
                            )
                        }
                        Row {
                            IconButton(onClick = { editingPlan = plan }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit plan", tint = VeloraOnBackground)
                            }
                            if (!plan.isPermanentFree && plan.id != "free") {
                                IconButton(onClick = { onArchivePlan(plan, !plan.isArchived) }) {
                                    Icon(
                                        Icons.Filled.Archive,
                                        contentDescription = if (plan.isArchived) "Unarchive" else "Archive",
                                        tint = if (plan.isArchived) VeloraMatchGreen else VeloraCoral
                                    )
                                }
                            }
                        }
                    }
                    Text(plan.description, style = MaterialTheme.typography.bodySmall, color = VeloraOnSurfaceMuted)
                }
            }
        }
    }

    if (creatingNew || editingPlan != null) {
        PlanEditorDialog(
            initial = editingPlan,
            onDismiss = {
                creatingNew = false
                editingPlan = null
            },
            onSave = { updated ->
                onSavePlan(updated) {
                    creatingNew = false
                    editingPlan = null
                }
            }
        )
    }
}

@Composable
private fun PlanEditorDialog(
    initial: SubscriptionPlan?,
    onDismiss: () -> Unit,
    onSave: (SubscriptionPlan) -> Unit
) {
    val seed = remember(initial) {
        initial ?: SubscriptionPlan(
            billingInterval = "monthly",
            maxQuality = "1080p",
            maxSimultaneousStreams = 2,
            maxProfiles = 5,
            isActive = true,
            isPublic = true
        )
    }
    val isPermFree = seed.isPermanentFree || seed.id == "free"
    var name by remember { mutableStateOf(seed.name) }
    var description by remember { mutableStateOf(seed.description) }
    var monthlyDollars by remember { mutableStateOf(String.format(Locale.US, "%.2f", seed.monthlyPriceCents / 100.0)) }
    var yearlyDollars by remember { mutableStateOf(String.format(Locale.US, "%.2f", seed.yearlyPriceCents / 100.0)) }
    var billingInterval by remember { mutableStateOf(seed.billingInterval) }
    var maxQuality by remember { mutableStateOf(seed.maxQuality) }
    var maxStreams by remember { mutableStateOf(seed.maxSimultaneousStreams.toString()) }
    var maxProfiles by remember { mutableStateOf(seed.maxProfiles.toString()) }
    var isActive by remember { mutableStateOf(seed.isActive) }
    var isPublic by remember { mutableStateOf(seed.isPublic) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VeloraSurfaceElevated,
        title = { Text(if (initial == null) "Create Subscription Plan" else "Edit ${initial.name}", color = VeloraOnBackground) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Plan Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, minLines = 2, modifier = Modifier.fillMaxWidth())

                if (!isPermFree) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = monthlyDollars, onValueChange = { monthlyDollars = it }, label = { Text("Monthly ($)") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = yearlyDollars, onValueChange = { yearlyDollars = it }, label = { Text("Yearly ($)") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("free", "monthly", "yearly", "custom").forEach { interval ->
                            FilterChip(selected = billingInterval == interval, onClick = { billingInterval = interval }, label = { Text(interval) })
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = maxQuality, onValueChange = { maxQuality = it }, label = { Text("Max Quality") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = maxStreams, onValueChange = { maxStreams = it }, label = { Text("Streams") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = maxProfiles, onValueChange = { maxProfiles = it }, label = { Text("Profiles") }, singleLine = true, modifier = Modifier.weight(1f))
                }

                if (!isPermFree) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Plan Enabled (Active)", color = VeloraOnBackground)
                        Switch(checked = isActive, onCheckedChange = { isActive = it })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Publicly Visible to Viewers", color = VeloraOnBackground)
                        Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    val mCents = ((monthlyDollars.toDoubleOrNull() ?: 0.0) * 100.0).toInt().coerceAtLeast(0)
                    val yCents = ((yearlyDollars.toDoubleOrNull() ?: 0.0) * 100.0).toInt().coerceAtLeast(0)
                    val allowedQuality = if (maxQuality in setOf("Auto", "480p", "720p", "1080p", "4K")) maxQuality else "1080p"
                    onSave(
                        SubscriptionPlan(
                            id = initial?.id ?: "",
                            tierCode = initial?.tierCode ?: name.lowercase().replace(Regex("[^a-z0-9]+"), "_").take(32),
                            name = name.trim(),
                            description = description.trim().ifBlank { "Notflix subscription plan" },
                            isPermanentFree = isPermFree,
                            isActive = if (isPermFree) true else isActive,
                            isPublic = if (isPermFree) true else isPublic,
                            isArchived = initial?.isArchived ?: false,
                            billingInterval = if (isPermFree) "free" else billingInterval,
                            monthlyPriceCents = if (isPermFree) 0 else mCents,
                            yearlyPriceCents = if (isPermFree) 0 else yCents,
                            maxQuality = allowedQuality,
                            maxSimultaneousStreams = maxStreams.toIntOrNull() ?: 2,
                            maxProfiles = maxProfiles.toIntOrNull() ?: 5,
                            featuresSummary = initial?.featuresSummary ?: listOf("Full streaming access"),
                            displayOrder = initial?.displayOrder ?: 10
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson)
            ) { Text("Save Plan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = VeloraOnSurfaceMuted) }
        }
    )
}

@Composable
fun AdminPlatformFreeAccessCard(
    platformSettings: PlatformSettings,
    onToggleFreeAccess: (Boolean) -> Unit
) {
    Surface(
        color = VeloraMatchGreen.copy(alpha = 0.12f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, VeloraMatchGreen.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = VeloraMatchGreen)
                    Column {
                        Text(
                            "Platform-Wide Free Access Mode",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = VeloraOnBackground
                        )
                        Text(
                            text = if (platformSettings.freeAccessEnabled) "ENABLED — Free access active across Notflix" else "DISABLED — Plan-based access active",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (platformSettings.freeAccessEnabled) VeloraMatchGreen else VeloraGold
                        )
                    }
                }
                Switch(
                    checked = platformSettings.freeAccessEnabled,
                    onCheckedChange = onToggleFreeAccess,
                    modifier = Modifier.testTag("admin_free_access_switch")
                )
            }
            Text(
                "Toggling Free Access never deletes, resets, or overwrites any user's stored subscription plan or history.",
                style = MaterialTheme.typography.bodySmall,
                color = VeloraOnSurfaceMuted
            )
        }
    }
}

@Composable
fun AdminAuditLogTab(auditLogs: List<AuditLogEntry>) {
    val formatter = remember { SimpleDateFormat("MMM d, yyyy HH:mm", Locale.US) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(
                color = VeloraSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, VeloraOutline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.History, contentDescription = null, tint = VeloraGold)
                    Column {
                        Text("Immutable Audit Log (${auditLogs.size})", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = VeloraOnBackground)
                        Text("Append-only record of role changes, moderation decisions, subscription updates, and platform toggles.", style = MaterialTheme.typography.bodySmall, color = VeloraOnSurfaceMuted)
                    }
                }
            }
        }

        if (auditLogs.isEmpty()) {
            item {
                Text("No audit entries recorded yet.", style = MaterialTheme.typography.bodyMedium, color = VeloraOnSurfaceMuted, modifier = Modifier.padding(12.dp))
            }
        } else {
            itemsIndexed(auditLogs, key = { idx, log -> "${log.id.ifBlank { "log" }}_$idx" }) { _, log ->
                val dateStr = log.createdAt?.toDate()?.let { formatter.format(it) } ?: "Just now"
                Surface(
                    color = VeloraSurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, VeloraOutline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(log.action, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = VeloraGold)
                            Text(dateStr, style = MaterialTheme.typography.labelSmall, color = VeloraOnSurfaceMuted)
                        }
                        Text(
                            text = "Actor: ${log.actorRole.uppercase()} (${log.actorUid.take(10)}…)" +
                                (log.targetUid?.let { " • Target: ${it.take(10)}…" } ?: "") +
                                (log.targetContentId?.let { " • Content: $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = VeloraOnBackground
                        )
                        if (log.previousValue.isNotBlank() || log.newValue.isNotBlank()) {
                            Text(
                                text = "${log.previousValue} → ${log.newValue}",
                                style = MaterialTheme.typography.labelSmall,
                                color = VeloraKidsCyan
                            )
                        }
                        if (log.details.isNotBlank()) {
                            Text(log.details, style = MaterialTheme.typography.bodySmall, color = VeloraOnSurfaceMuted)
                        }
                    }
                }
            }
        }
    }
}
