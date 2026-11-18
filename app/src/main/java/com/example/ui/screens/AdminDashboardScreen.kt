package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CatalogItem
import com.example.data.model.Episode
import com.example.data.model.Genre
import com.example.data.model.HomepageRowRule
import com.example.data.model.HomepageSettings
import com.example.data.model.Season
import com.example.data.model.SeasonDraft
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UserAccount
import com.example.data.model.UserSubscription
import com.example.data.repository.MediaAssetCategory
import com.example.data.repository.MediaUploadState
import com.example.ui.state.HomeFeedRowBuilder
import com.example.ui.theme.VeloraBackground
import com.example.ui.theme.VeloraCoral
import com.example.ui.theme.VeloraCrimson
import com.example.ui.theme.VeloraError
import com.example.ui.theme.VeloraGold
import com.example.ui.theme.VeloraMatchGreen
import com.example.ui.theme.VeloraOnBackground
import com.example.ui.theme.VeloraOnSurfaceMuted
import com.example.ui.theme.VeloraOutline
import com.example.ui.theme.VeloraSurfaceElevated
import com.example.util.DetectedSourceKind
import com.example.util.VideoSourceType
import com.example.util.VideoSourceUtils

data class AdminDashboardState(
    val isAdmin: Boolean,
    val actorRole: com.example.data.model.AppRole = com.example.data.model.AppRole.ADMIN,
    val catalog: List<CatalogItem> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val users: List<UserAccount> = emptyList(),
    val subscriptionPlans: List<SubscriptionPlan> = emptyList(),
    val userSubscriptions: List<UserSubscription> = emptyList(),
    val submissions: List<com.example.data.model.ContentSubmission> = emptyList(),
    val publisherApplications: List<com.example.data.model.PublisherApplication> = emptyList(),
    val platformSettings: com.example.data.model.PlatformSettings = com.example.data.model.PlatformSettings(),
    val auditLogs: List<com.example.data.model.AuditLogEntry> = emptyList(),
    val isSavingCatalog: Boolean = false,
    val isLoadingSeriesSeasons: Boolean = false,
    val loadedSeriesSeasons: List<SeasonDraft> = emptyList(),
    val adminErrorMessage: String? = null,
    val mediaUploadStates: Map<String, MediaUploadState> = emptyMap(),
    val homepageSettings: HomepageSettings = HomepageSettings(),
    val homepageRowRules: List<HomepageRowRule> = HomeFeedRowBuilder.defaultRowRules()
)

class AdminDashboardActions(
    val onUploadMedia: ((fieldKey: String, uri: Uri, category: MediaAssetCategory, contentFolderHint: String, onUploaded: (String) -> Unit) -> Unit)? = null,
    val onClearUploadState: ((String?) -> Unit)? = null,
    val onSeedCatalog: () -> Unit = {},
    val onSyncSubscriptionSchemas: () -> Unit = {},
    val onPrepareSeriesEditor: (String?) -> Unit = {},
    val onSaveCatalogItem: (CatalogItem, () -> Unit) -> Unit = { _, done -> done() },
    val onSaveSeriesWithSeasons: (
        series: CatalogItem,
        seasons: List<SeasonDraft>,
        removedSeasonIds: Set<String>,
        removedEpisodeRefs: List<Pair<String, String>>,
        onComplete: () -> Unit
    ) -> Unit = { _, _, _, _, done -> done() },
    val onDeleteCatalogItem: (String, String) -> Unit = { _, _ -> },
    val onAddSeason: (contentId: String, Season) -> Unit = { _, _ -> },
    val onAddEpisode: (contentId: String, seasonId: String, Episode) -> Unit = { _, _, _ -> },
    val onSaveGenre: (Genre) -> Unit = {},
    val onDeleteGenre: (String) -> Unit = {},
    val onSaveHomepageSettings: (HomepageSettings) -> Unit = {},
    val onSaveHomepageRowRules: (List<HomepageRowRule>) -> Unit = {},
    val onResetHomepageLayout: () -> Unit = {},
    val onAssignUserRole: (UserAccount, com.example.data.model.AppRole, com.example.data.model.PublisherStatus?) -> Unit = { _, _, _ -> },
    val onToggleUserSuspended: (UserAccount, Boolean) -> Unit = { _, _ -> },
    val onReviewPublisherApplication: (com.example.data.model.PublisherApplication, com.example.data.model.PublisherStatus, String) -> Unit = { _, _, _ -> },
    val onUpdatePublisherStatus: (UserAccount, com.example.data.model.PublisherStatus) -> Unit = { _, _ -> },
    val observeReviewHistory: (String) -> kotlinx.coroutines.flow.Flow<List<com.example.data.model.ContentReviewDecision>> = { kotlinx.coroutines.flow.flowOf(emptyList()) },
    val onReviewSubmission: (
        submission: com.example.data.model.ContentSubmission,
        decision: com.example.data.model.ContentReviewStatus,
        notes: String,
        videoVerified: Boolean,
        rightsConfirmed: Boolean,
        metadataVerified: Boolean,
        onComplete: () -> Unit
    ) -> Unit = { _, _, _, _, _, _, done -> done() },
    val onSaveSubscriptionPlan: (SubscriptionPlan, () -> Unit) -> Unit = { _, done -> done() },
    val onArchiveSubscriptionPlan: (SubscriptionPlan, Boolean) -> Unit = { _, _ -> },
    val onAssignUserSubscription: (String, SubscriptionPlan, String) -> Unit = { _, _, _ -> },
    val onUpdateUserSubscriptionStatus: (String, String) -> Unit = { _, _ -> },
    val onToggleFreeAccess: (Boolean) -> Unit = {}
)

@Composable
fun AdminDashboardScreen(
    state: AdminDashboardState,
    actions: AdminDashboardActions,
    onBack: () -> Unit
) {
    if (!state.isAdmin) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VeloraBackground)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = VeloraError, modifier = Modifier.size(56.dp))
                Text("Administrator Access Required", style = MaterialTheme.typography.headlineMedium, color = VeloraOnBackground)
                Text("Only platform Administrators and the Owner can access this console.", style = MaterialTheme.typography.bodyMedium, color = VeloraOnSurfaceMuted)
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson)) {
                    Text("Return")
                }
            }
        }
        return
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var platformSubTab by rememberSaveable { mutableIntStateOf(0) } // 0=FreeAccess+Home, 1=Live Catalog, 2=Genres
    var editingItem by remember { mutableStateOf<CatalogItem?>(null) }
    var createMediaType by remember { mutableStateOf<String?>(null) }
    var createAsComingSoon by remember { mutableStateOf(false) }
    var managingSeries by remember { mutableStateOf<CatalogItem?>(null) }
    var showAddGenreDialog by remember { mutableStateOf(false) }

    val pendingSubmissionsCount = state.submissions.count {
        it.parsedStatus == com.example.data.model.ContentReviewStatus.PENDING_REVIEW ||
            it.parsedStatus == com.example.data.model.ContentReviewStatus.UNDER_REVIEW
    }
    val pendingPublisherAppsCount = state.publisherApplications.count {
        it.parsedStatus == com.example.data.model.PublisherStatus.PENDING ||
            it.parsedStatus == com.example.data.model.PublisherStatus.UNDER_REVIEW
    }

    val tabTitles = listOf(
        "Overview",
        if (pendingSubmissionsCount > 0) "Content Review ($pendingSubmissionsCount)" else "Content Review",
        "Movie Submissions",
        "TV Show Submissions",
        "Users",
        if (pendingPublisherAppsCount > 0) "Publishers ($pendingPublisherAppsCount)" else "Publishers",
        "Subscriptions",
        "Plans & Pricing",
        "Platform Settings",
        "Audit Log"
    )

    Scaffold(
        containerColor = VeloraBackground,
        floatingActionButton = {
            if (selectedTab == 8 && platformSubTab == 1) {
                FloatingActionButton(
                    onClick = { createMediaType = "movie" },
                    containerColor = VeloraCrimson,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("admin_add_content_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Movie or TV Show")
                }
            } else if (selectedTab == 8 && platformSubTab == 2) {
                FloatingActionButton(
                    onClick = { showAddGenreDialog = true },
                    containerColor = VeloraCrimson,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Genre")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("admin_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = VeloraOnBackground)
                    }
                    Column {
                        Text(
                            text = if (state.actorRole.isOwner) "Owner & Admin Console" else "Admin Console",
                            style = MaterialTheme.typography.headlineSmall,
                            color = VeloraOnBackground
                        )
                        Text(
                            text = if (state.actorRole.isOwner) "Full Owner Governance & Content Moderation" else "Content Moderation & Platform Administration",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.actorRole.isOwner) VeloraGold else VeloraOnSurfaceMuted
                        )
                    }
                }
                TextButton(onClick = actions.onSeedCatalog) {
                    Icon(Icons.Filled.CloudSync, contentDescription = null, tint = VeloraGold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Sync Seed", color = VeloraGold)
                }
            }

            if (!state.adminErrorMessage.isNullOrBlank()) {
                Surface(
                    color = VeloraError.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, VeloraError.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("admin_error_banner")
                ) {
                    Text(
                        text = state.adminErrorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = VeloraError,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            androidx.compose.material3.ScrollableTabRow(
                selectedTabIndex = selectedTab.coerceIn(0, tabTitles.lastIndex),
                containerColor = VeloraBackground,
                contentColor = VeloraOnBackground,
                edgePadding = 12.dp,
                indicator = { positions ->
                    val activePos = positions.getOrNull(selectedTab.coerceIn(0, tabTitles.lastIndex))
                    if (activePos != null) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(activePos),
                            color = VeloraCrimson
                        )
                    }
                }
            ) {
                tabTitles.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = { Text(title, maxLines = 1) }
                    )
                }
            }

            when (selectedTab) {
                0 -> AdminOverviewTab(
                    state = state,
                    pendingSubmissionsCount = pendingSubmissionsCount,
                    pendingPublisherAppsCount = pendingPublisherAppsCount,
                    onOpenReviewQueue = { selectedTab = 1 }
                )
                1 -> AdminContentReviewTab(
                    submissions = state.submissions,
                    mediaTypeFilter = null,
                    isSaving = state.isSavingCatalog,
                    observeReviewHistory = actions.observeReviewHistory,
                    onReviewSubmission = actions.onReviewSubmission
                )
                2 -> AdminContentReviewTab(
                    submissions = state.submissions,
                    mediaTypeFilter = "movies",
                    isSaving = state.isSavingCatalog,
                    observeReviewHistory = actions.observeReviewHistory,
                    onReviewSubmission = actions.onReviewSubmission
                )
                3 -> AdminContentReviewTab(
                    submissions = state.submissions,
                    mediaTypeFilter = "series",
                    isSaving = state.isSavingCatalog,
                    observeReviewHistory = actions.observeReviewHistory,
                    onReviewSubmission = actions.onReviewSubmission
                )
                4 -> AdminUserManagementTab(
                    actorRole = state.actorRole,
                    users = state.users,
                    onAssignRole = actions.onAssignUserRole,
                    onToggleSuspended = actions.onToggleUserSuspended
                )
                5 -> AdminPublisherManagementTab(
                    applications = state.publisherApplications,
                    users = state.users,
                    onReviewApplication = actions.onReviewPublisherApplication,
                    onUpdatePublisherStatus = actions.onUpdatePublisherStatus
                )
                6 -> AdminUserSubscriptionsTab(
                    users = state.users,
                    plans = state.subscriptionPlans,
                    userSubscriptions = state.userSubscriptions,
                    onAssignPlan = actions.onAssignUserSubscription,
                    onUpdateStatus = actions.onUpdateUserSubscriptionStatus
                )
                7 -> AdminPlanPricingTab(
                    plans = state.subscriptionPlans,
                    onSyncSchemas = actions.onSyncSubscriptionSchemas,
                    onSavePlan = actions.onSaveSubscriptionPlan,
                    onArchivePlan = actions.onArchiveSubscriptionPlan
                )
                8 -> AdminPlatformSettingsTab(
                    state = state,
                    actions = actions,
                    platformSubTab = platformSubTab,
                    onSelectPlatformSubTab = { platformSubTab = it },
                    onStartCreateMovie = { comingSoon ->
                        createAsComingSoon = comingSoon
                        createMediaType = "movie"
                    },
                    onStartCreateSeries = {
                        actions.onPrepareSeriesEditor(null)
                        createMediaType = "series"
                    },
                    onEditItem = { item ->
                        if (item.isSeries) actions.onPrepareSeriesEditor(item.id)
                        editingItem = item
                    },
                    onQuickManageSeries = { managingSeries = it }
                )
                9 -> AdminAuditLogTab(auditLogs = state.auditLogs)
            }
        }
    }

    if (createMediaType != null || editingItem != null) {
        val activeEditing = editingItem
        AdminCatalogEditorDialog(
            initial = activeEditing,
            initialMediaType = activeEditing?.mediaType ?: (createMediaType ?: "movie"),
            initialIsComingSoon = activeEditing == null && createAsComingSoon,
            availableGenres = state.genres,
            loadedSeriesSeasons = state.loadedSeriesSeasons,
            isLoadingSeriesSeasons = state.isLoadingSeriesSeasons,
            isSaving = state.isSavingCatalog,
            mediaUploadStates = state.mediaUploadStates,
            onUploadMedia = actions.onUploadMedia,
            onClearUploadState = actions.onClearUploadState,
            onDismiss = {
                actions.onClearUploadState?.invoke(null)
                createMediaType = null
                createAsComingSoon = false
                editingItem = null
            },
            onSaveMovie = { itemToSave ->
                actions.onSaveCatalogItem(itemToSave) {
                    actions.onClearUploadState?.invoke(null)
                    createMediaType = null
                    createAsComingSoon = false
                    editingItem = null
                }
            },
            onSaveSeries = { series, seasonDrafts, removedSeasonIds, removedEpisodeRefs ->
                actions.onSaveSeriesWithSeasons(series, seasonDrafts, removedSeasonIds, removedEpisodeRefs) {
                    actions.onClearUploadState?.invoke(null)
                    createMediaType = null
                    createAsComingSoon = false
                    editingItem = null
                }
            }
        )
    }

    managingSeries?.let { series ->
        SeriesEpisodeManagerDialog(
            series = series,
            onDismiss = { managingSeries = null },
            onAddSeason = { season -> actions.onAddSeason(series.id, season) },
            onAddEpisode = { seasonId, ep -> actions.onAddEpisode(series.id, seasonId, ep) }
        )
    }

    if (showAddGenreDialog) {
        var name by remember { mutableStateOf("") }
        var order by remember { mutableStateOf("9") }
        AlertDialog(
            onDismissRequest = { showAddGenreDialog = false },
            containerColor = VeloraSurfaceElevated,
            title = { Text("Add Genre", color = VeloraOnBackground) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Genre Name") }, singleLine = true)
                    OutlinedTextField(value = order, onValueChange = { order = it }, label = { Text("Display Order") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            actions.onSaveGenre(Genre(name = name.trim(), slug = name.trim(), displayOrder = order.toIntOrNull() ?: 9, isActive = true))
                            showAddGenreDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddGenreDialog = false }) { Text("Cancel", color = VeloraOnSurfaceMuted) }
            }
        )
    }
}

@Composable
private fun AdminOverviewTab(
    state: AdminDashboardState,
    pendingSubmissionsCount: Int,
    pendingPublisherAppsCount: Int,
    onOpenReviewQueue: () -> Unit
) {
    val movieCount = state.catalog.count { !it.isSeries }
    val seriesCount = state.catalog.count { it.isSeries }
    val visibleCount = state.catalog.count { it.isVisible }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                color = VeloraCrimson.copy(alpha = 0.14f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, VeloraCrimson.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Content Review Queue: $pendingSubmissionsCount Pending",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = VeloraOnBackground
                    )
                    Text(
                        text = "Review and approve or reject Publisher submissions before they become publicly visible on Notflix.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VeloraOnSurfaceMuted
                    )
                    Button(
                        onClick = onOpenReviewQueue,
                        colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson),
                        modifier = Modifier.testTag("admin_overview_open_review_button")
                    ) {
                        Text("Open Content Review Queue")
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("Pending Reviews", pendingSubmissionsCount.toString(), Modifier.weight(1f))
                StatCard("Publisher Apps", pendingPublisherAppsCount.toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("Live Movies", movieCount.toString(), Modifier.weight(1f))
                StatCard("Live TV Shows", seriesCount.toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("Visible Titles", "$visibleCount/${state.catalog.size}", Modifier.weight(1f))
                StatCard("Users", state.users.size.coerceAtLeast(1).toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("Free Access", if (state.platformSettings.freeAccessEnabled) "ON" else "OFF", Modifier.weight(1f))
                StatCard("Audit Entries", state.auditLogs.size.toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AdminPlatformSettingsTab(
    state: AdminDashboardState,
    actions: AdminDashboardActions,
    platformSubTab: Int,
    onSelectPlatformSubTab: (Int) -> Unit,
    onStartCreateMovie: (comingSoon: Boolean) -> Unit,
    onStartCreateSeries: () -> Unit,
    onEditItem: (CatalogItem) -> Unit,
    onQuickManageSeries: (CatalogItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Free Access & Home", "Live Catalog", "Genres").forEachIndexed { idx, label ->
                androidx.compose.material3.FilterChip(
                    selected = platformSubTab == idx,
                    onClick = { onSelectPlatformSubTab(idx) },
                    label = { Text(label) }
                )
            }
        }

        when (platformSubTab) {
            0 -> Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    AdminPlatformFreeAccessCard(
                        platformSettings = state.platformSettings,
                        onToggleFreeAccess = actions.onToggleFreeAccess
                    )
                }
                AdminHomepageTab(
                    catalog = state.catalog,
                    genres = state.genres,
                    homepageSettings = state.homepageSettings,
                    homepageRowRules = state.homepageRowRules,
                    onSaveHomepageSettings = actions.onSaveHomepageSettings,
                    onSaveHomepageRowRules = actions.onSaveHomepageRowRules,
                    onResetHomepageLayout = actions.onResetHomepageLayout,
                    onSaveCatalogItem = actions.onSaveCatalogItem,
                    onAddComingSoonTitle = { onStartCreateMovie(true) }
                )
            }
            1 -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onStartCreateMovie(false) },
                            colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_add_movie_button")
                        ) {
                            Icon(Icons.Filled.Movie, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add Movie")
                        }
                        OutlinedButton(
                            onClick = onStartCreateSeries,
                            border = BorderStroke(1.dp, VeloraCrimson),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_add_series_button")
                        ) {
                            Icon(Icons.Filled.LiveTv, contentDescription = null, tint = VeloraCrimson, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add TV Show", color = VeloraCrimson)
                        }
                    }
                }
                itemsIndexed(state.catalog, key = { idx, item -> "${item.id.ifBlank { "cat" }}_$idx" }) { _, item ->
                    val resolvedSummary = remember(item.id, item.videoUrl, item.hlsUrl, item.videoSourceType, item.providerName) {
                        VideoSourceUtils.resolveForPlayback(
                            item.preferredStreamUrl,
                            item.videoSourceType,
                            item.providerName
                        )
                    }
                    val badgeText = item.providerName?.takeIf { it.isNotBlank() }
                        ?: resolvedSummary.providerName?.takeIf { it.isNotBlank() }
                        ?: VideoSourceType.fromKey(item.videoSourceType).label
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
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(item.title, style = MaterialTheme.typography.titleMedium, color = VeloraOnBackground)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "${if (item.isSeries) "TV Show" else "Movie"} • ${item.releaseYear} • ${item.ageRating}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = VeloraOnSurfaceMuted
                                        )
                                        Surface(color = VeloraCoral.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
                                            Text(badgeText, style = MaterialTheme.typography.labelSmall, color = VeloraCoral, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                                Row {
                                    IconButton(onClick = { actions.onSaveCatalogItem(item.copy(isFeatured = !item.isFeatured)) {} }) {
                                        Icon(Icons.Filled.Star, contentDescription = "Feature on home", tint = if (item.isFeatured) VeloraGold else VeloraOnSurfaceMuted)
                                    }
                                    IconButton(onClick = { actions.onSaveCatalogItem(item.copy(isVisible = !item.isVisible)) {} }) {
                                        Icon(if (item.isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = "Toggle visibility", tint = if (item.isVisible) VeloraMatchGreen else VeloraError)
                                    }
                                    IconButton(onClick = { onEditItem(item) }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = VeloraOnBackground)
                                    }
                                    IconButton(onClick = { actions.onDeleteCatalogItem(item.id, item.title) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = VeloraError)
                                    }
                                }
                            }
                            if (item.isSeries) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { onEditItem(item) },
                                        border = BorderStroke(1.dp, VeloraOutline)
                                    ) { Text("Manage Seasons & Episodes", color = VeloraOnBackground) }
                                    OutlinedButton(
                                        onClick = { onQuickManageSeries(item) },
                                        border = BorderStroke(1.dp, VeloraOutline)
                                    ) { Text("Quick Add Ep", color = VeloraGold) }
                                }
                            }
                        }
                    }
                }
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(state.genres, key = { idx, genre -> "${genre.id.ifBlank { "genre" }}_$idx" }) { _, genre ->
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
                            Column {
                                Text(genre.name, style = MaterialTheme.typography.titleMedium, color = VeloraOnBackground)
                                Text("Order: ${genre.displayOrder} • ${if (genre.isActive) "Active" else "Hidden"}", style = MaterialTheme.typography.bodySmall, color = VeloraOnSurfaceMuted)
                            }
                            Row {
                                Switch(checked = genre.isActive, onCheckedChange = { actions.onSaveGenre(genre.copy(isActive = it)) })
                                IconButton(onClick = { actions.onDeleteGenre(genre.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete genre", tint = VeloraError)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = VeloraSurfaceElevated,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, VeloraOutline),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold), color = VeloraCrimson)
            Text(label, style = MaterialTheme.typography.bodySmall, color = VeloraOnSurfaceMuted)
        }
    }
}

@Composable
private fun SeriesEpisodeManagerDialog(
    series: CatalogItem,
    onDismiss: () -> Unit,
    onAddSeason: (Season) -> Unit,
    onAddEpisode: (seasonId: String, Episode) -> Unit
) {
    var seasonNum by remember { mutableStateOf("1") }
    var seasonTitle by remember { mutableStateOf("Season 1") }
    var epNum by remember { mutableStateOf("1") }
    var epTitle by remember { mutableStateOf("") }
    var epSynopsis by remember { mutableStateOf("") }
    var epRuntime by remember { mutableStateOf("42") }
    var epSourceType by remember { mutableStateOf(VideoSourceType.AUTO) }
    var epVideoUrl by remember { mutableStateOf("") }
    var epProviderName by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VeloraSurfaceElevated,
        title = { Text("Quick Add Episode • ${series.title}", color = VeloraOnBackground) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Season", style = MaterialTheme.typography.titleSmall, color = VeloraGold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = seasonNum,
                        onValueChange = { seasonNum = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("#") },
                        modifier = Modifier.width(68.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = seasonTitle,
                        onValueChange = { seasonTitle = it },
                        label = { Text("Season Title") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedButton(onClick = {
                    val sNum = seasonNum.toIntOrNull() ?: 1
                    onAddSeason(
                        Season(
                            id = "season-$sNum",
                            seasonNumber = sNum,
                            title = seasonTitle.trim().ifBlank { "Season $sNum" },
                            year = series.releaseYear,
                            isVisible = true
                        )
                    )
                }) { Text("Save Season", color = VeloraOnBackground) }

                Spacer(Modifier.height(6.dp))
                Text("Add Episode to Season ${seasonNum.ifBlank { "1" }}", style = MaterialTheme.typography.titleSmall, color = VeloraGold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = epNum,
                        onValueChange = { epNum = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("Ep #*") },
                        modifier = Modifier.width(76.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = epRuntime,
                        onValueChange = { epRuntime = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("Mins*") },
                        modifier = Modifier.width(86.dp),
                        singleLine = true
                    )
                }
                OutlinedTextField(value = epTitle, onValueChange = { epTitle = it }, label = { Text("Episode Title*") }, singleLine = true)
                OutlinedTextField(value = epSynopsis, onValueChange = { epSynopsis = it }, label = { Text("Episode Description*") }, minLines = 2)

                VideoSourceSelectorRow(
                    selectedType = epSourceType,
                    onSelectType = { epSourceType = it },
                    tagPrefix = "quick_ep_type"
                )
                OutlinedTextField(
                    value = epVideoUrl,
                    onValueChange = { epVideoUrl = it },
                    label = { Text("Episode Video / Webpage URL*") },
                    placeholder = { Text(placeholderForSourceType(epSourceType)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = epProviderName,
                    onValueChange = { epProviderName = it.take(80) },
                    label = { Text("Provider Name (Optional)") },
                    placeholder = { Text("e.g. YouTube, Vimeo, CDN") },
                    singleLine = true
                )
                SourceValidationBadge(
                    url = epVideoUrl,
                    sourceType = epSourceType,
                    customProviderName = epProviderName,
                    required = true
                )
                errorMsg?.let { Text(it, color = VeloraError, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sNum = seasonNum.toIntOrNull()
                    val eNum = epNum.toIntOrNull()
                    val mins = epRuntime.toIntOrNull()
                    val check = VideoSourceUtils.validateVideoSource(epVideoUrl, epSourceType, required = true)
                    when {
                        sNum == null || sNum < 1 -> errorMsg = "Enter a valid season number."
                        eNum == null || eNum < 1 -> errorMsg = "Enter a valid episode number."
                        epTitle.isBlank() -> errorMsg = "Episode title is required."
                        mins == null || mins < 1 -> errorMsg = "Enter runtime in minutes."
                        !check.isValid -> errorMsg = check.errorOrNull
                        else -> {
                            val cleanUrl = epVideoUrl.trim()
                            val kind = VideoSourceUtils.detectClassification(cleanUrl, epProviderName).detectedKind
                            onAddEpisode(
                                "season-$sNum",
                                Episode(
                                    id = "s${sNum}e$eNum",
                                    seasonNumber = sNum,
                                    episodeNumber = eNum,
                                    title = epTitle.trim(),
                                    synopsis = epSynopsis.trim().ifBlank { "Episode $eNum" },
                                    runtimeMinutes = mins,
                                    thumbnailKey = series.backdropKey,
                                    videoUrl = cleanUrl,
                                    videoSourceType = epSourceType.key,
                                    providerName = epProviderName.trim().takeIf { it.isNotBlank() },
                                    hlsUrl = if (epSourceType == VideoSourceType.HLS || kind == DetectedSourceKind.HLS_STREAM) cleanUrl else null,
                                    isVisible = true
                                )
                            )
                            onDismiss()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson)
            ) { Text("Add Episode") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = VeloraOnSurfaceMuted) }
        }
    )
}
