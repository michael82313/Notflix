package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import com.example.data.model.Season
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UserAccount
import com.example.data.model.UserSubscription
import com.example.ui.theme.VeloraBackground
import com.example.ui.theme.VeloraCrimson
import com.example.ui.theme.VeloraError
import com.example.ui.theme.VeloraGold
import com.example.ui.theme.VeloraMatchGreen
import com.example.ui.theme.VeloraOnBackground
import com.example.ui.theme.VeloraOnSurfaceMuted
import com.example.ui.theme.VeloraOutline
import com.example.ui.theme.VeloraSurfaceElevated

@Composable
fun AdminDashboardScreen(
    isAdmin: Boolean,
    catalog: List<CatalogItem>,
    genres: List<Genre>,
    users: List<UserAccount>,
    subscriptionPlans: List<SubscriptionPlan>,
    userSubscriptions: List<UserSubscription>,
    onBack: () -> Unit,
    onSeedCatalog: () -> Unit,
    onSyncSubscriptionSchemas: () -> Unit,
    onSaveCatalogItem: (CatalogItem) -> Unit,
    onDeleteCatalogItem: (String, String) -> Unit,
    onAddSeason: (contentId: String, Season) -> Unit,
    onAddEpisode: (contentId: String, seasonId: String, Episode) -> Unit,
    onSaveGenre: (Genre) -> Unit,
    onDeleteGenre: (String) -> Unit,
    onToggleUserAdmin: (UserAccount, Boolean) -> Unit
) {
    if (!isAdmin) {
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
                Text("Regular member accounts cannot access platform administration tools.", style = MaterialTheme.typography.bodyMedium, color = VeloraOnSurfaceMuted)
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson)) {
                    Text("Return")
                }
            }
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var editingItem by remember { mutableStateOf<CatalogItem?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var managingSeries by remember { mutableStateOf<CatalogItem?>(null) }
    var showAddGenreDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = VeloraBackground,
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = VeloraCrimson,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("admin_add_content_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Movie or TV Show")
                }
            } else if (selectedTab == 2) {
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
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = VeloraOnBackground)
                    }
                    Text("Admin Console", style = MaterialTheme.typography.headlineMedium, color = VeloraOnBackground)
                }
                TextButton(onClick = onSeedCatalog) {
                    Icon(Icons.Filled.CloudSync, contentDescription = null, tint = VeloraGold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Sync Seed", color = VeloraGold)
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = VeloraBackground,
                contentColor = VeloraOnBackground,
                indicator = { positions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(positions[selectedTab]),
                        color = VeloraCrimson
                    )
                }
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Stats") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Catalog (${catalog.size})") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Genres") })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Users") })
                Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 }, text = { Text("Plans") })
            }

            when (selectedTab) {
                0 -> {
                    val movieCount = catalog.count { !it.isSeries }
                    val seriesCount = catalog.count { it.isSeries }
                    val featuredCount = catalog.count { it.isFeatured }
                    val visibleCount = catalog.count { it.isVisible }

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                StatCard("Total Movies", movieCount.toString(), Modifier.weight(1f))
                                StatCard("TV Series", seriesCount.toString(), Modifier.weight(1f))
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                StatCard("Featured Hero", featuredCount.toString(), Modifier.weight(1f))
                                StatCard("Visible Titles", "$visibleCount/${catalog.size}", Modifier.weight(1f))
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                StatCard("Active Genres", genres.count { it.isActive }.toString(), Modifier.weight(1f))
                                StatCard("Users", users.size.coerceAtLeast(1).toString(), Modifier.weight(1f))
                            }
                        }
                    }
                }
                1 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(catalog, key = { it.id }) { item ->
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
                                            Text(item.title, style = MaterialTheme.typography.titleMedium, color = VeloraOnBackground)
                                            Text(
                                                text = "${if (item.isSeries) "Series" else "Movie"} • ${item.releaseYear} • ${item.ageRating}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = VeloraOnSurfaceMuted
                                            )
                                        }
                                        Row {
                                            IconButton(onClick = {
                                                onSaveCatalogItem(item.copy(isFeatured = !item.isFeatured))
                                            }) {
                                                Icon(
                                                    Icons.Filled.Star,
                                                    contentDescription = "Feature on home",
                                                    tint = if (item.isFeatured) VeloraGold else VeloraOnSurfaceMuted
                                                )
                                            }
                                            IconButton(onClick = {
                                                onSaveCatalogItem(item.copy(isVisible = !item.isVisible))
                                            }) {
                                                Icon(
                                                    imageVector = if (item.isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                                    contentDescription = "Toggle visibility",
                                                    tint = if (item.isVisible) VeloraMatchGreen else VeloraError
                                                )
                                            }
                                            IconButton(onClick = { editingItem = item }) {
                                                Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = VeloraOnBackground)
                                            }
                                            IconButton(onClick = { onDeleteCatalogItem(item.id, item.title) }) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = VeloraError)
                                            }
                                        }
                                    }
                                    if (item.isSeries) {
                                        OutlinedButton(
                                            onClick = { managingSeries = item },
                                            border = BorderStroke(1.dp, VeloraOutline)
                                        ) {
                                            Text("Add Season / Episode", color = VeloraOnBackground)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(genres, key = { it.id }) { genre ->
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
                                        Switch(
                                            checked = genre.isActive,
                                            onCheckedChange = { onSaveGenre(genre.copy(isActive = it)) }
                                        )
                                        IconButton(onClick = { onDeleteGenre(genre.id) }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete genre", tint = VeloraError)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(users, key = { it.userId }) { user ->
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
                                        Text(user.displayName, style = MaterialTheme.typography.titleMedium, color = VeloraOnBackground)
                                        Text(
                                            user.email.ifBlank { user.phoneNumber?.ifBlank { null } ?: user.userId },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = VeloraOnSurfaceMuted
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = user.role.uppercase(),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (user.role == "admin") VeloraGold else VeloraOnSurfaceMuted
                                        )
                                        Switch(
                                            checked = user.role == "admin",
                                            onCheckedChange = { onToggleUserAdmin(user, it) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                4 -> {
                    AdminSubscriptionArchitectureTab(
                        plans = subscriptionPlans,
                        userSubscriptions = userSubscriptions,
                        onSyncSchemas = onSyncSubscriptionSchemas
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CatalogEditorDialog(
            initial = null,
            onDismiss = { showCreateDialog = false },
            onSave = {
                onSaveCatalogItem(it)
                showCreateDialog = false
            }
        )
    }

    editingItem?.let { item ->
        CatalogEditorDialog(
            initial = item,
            onDismiss = { editingItem = null },
            onSave = {
                onSaveCatalogItem(it)
                editingItem = null
            }
        )
    }

    managingSeries?.let { series ->
        SeriesEpisodeManagerDialog(
            series = series,
            onDismiss = { managingSeries = null },
            onAddSeason = { season -> onAddSeason(series.id, season) },
            onAddEpisode = { seasonId, ep -> onAddEpisode(series.id, seasonId, ep) }
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
                            onSaveGenre(Genre(name = name.trim(), slug = name.trim(), displayOrder = order.toIntOrNull() ?: 9, isActive = true))
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
private fun AdminSubscriptionArchitectureTab(
    plans: List<SubscriptionPlan>,
    userSubscriptions: List<UserSubscription>,
    onSyncSchemas: () -> Unit
) {
    val upcomingAdminControls = listOf(
        "Create & Edit Subscription Tiers",
        "Set Monthly & Yearly Prices",
        "Configure Per-Plan Feature Flags",
        "Set Viewing Quality & Simultaneous Stream Limits",
        "Enable or Disable Future Paid Tiers",
        "Assign Plans to Specific Users"
    )

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Verified, contentDescription = null, tint = VeloraMatchGreen)
                            Text(
                                text = "Free-First Architecture",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = VeloraOnBackground
                            )
                        }
                        OutlinedButton(
                            onClick = onSyncSchemas,
                            border = BorderStroke(1.dp, VeloraMatchGreen.copy(alpha = 0.5f))
                        ) {
                            Text("Sync Schemas", color = VeloraMatchGreen)
                        }
                    }
                    Text(
                        text = "Notflix launches 100% free with every user enrolled in the permanent Free plan. Paid plans, pricing, and billing remain disabled until activated by admins.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VeloraOnSurfaceMuted
                    )
                }
            }
        }

        item {
            Text(
                text = "Prepared Tier Schemas (${plans.size})",
                style = MaterialTheme.typography.titleMedium,
                color = VeloraOnBackground
            )
        }

        items(plans, key = { it.id.ifBlank { it.tierCode } }) { plan ->
            Surface(
                color = VeloraSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (plan.isPermanentFree) VeloraMatchGreen.copy(alpha = 0.5f) else VeloraOutline
                )
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
                        Text(
                            text = plan.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = VeloraOnBackground
                        )
                        Surface(
                            color = if (plan.isPermanentFree) VeloraMatchGreen.copy(alpha = 0.16f) else VeloraGold.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (plan.isPermanentFree) "PERMANENT • ACTIVE" else "COMING SOON • INACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (plan.isPermanentFree) VeloraMatchGreen else VeloraGold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = plan.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = VeloraOnSurfaceMuted
                    )
                    Text(
                        text = "Quality Limit: ${plan.maxQuality} • Streams: ${plan.maxSimultaneousStreams} • Profiles: ${plan.maxProfiles}",
                        style = MaterialTheme.typography.labelMedium,
                        color = VeloraOnBackground
                    )
                }
            }
        }

        item {
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
                        Text(
                            text = "Admin Subscription Controls",
                            style = MaterialTheme.typography.titleMedium,
                            color = VeloraOnBackground
                        )
                        Surface(
                            color = VeloraGold.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "COMING SOON",
                                style = MaterialTheme.typography.labelSmall,
                                color = VeloraGold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    upcomingAdminControls.forEach { capability ->
                        Text(
                            text = "• $capability",
                            style = MaterialTheme.typography.bodySmall,
                            color = VeloraOnSurfaceMuted
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Enrolled User Subscriptions (${userSubscriptions.size})",
                style = MaterialTheme.typography.titleMedium,
                color = VeloraOnBackground
            )
        }

        items(userSubscriptions, key = { it.userId }) { sub ->
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
                        Text(sub.userId, style = MaterialTheme.typography.bodyMedium, color = VeloraOnBackground)
                        Text(
                            text = "Assigned by ${sub.assignedBy} • ${if (sub.expiresAt == null) "No expiration" else "Scheduled end"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = VeloraOnSurfaceMuted
                        )
                    }
                    Text(
                        text = "${sub.planName.uppercase()} • ${sub.status.uppercase()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = VeloraMatchGreen
                    )
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
private fun CatalogEditorDialog(
    initial: CatalogItem?,
    onDismiss: () -> Unit,
    onSave: (CatalogItem) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var synopsis by remember { mutableStateOf(initial?.synopsis ?: "") }
    var mediaType by remember { mutableStateOf(initial?.mediaType ?: "movie") }
    var genresText by remember { mutableStateOf(initial?.genres?.joinToString(", ") ?: "Sci-Fi, Thriller") }
    var releaseYear by remember { mutableStateOf((initial?.releaseYear ?: 2026).toString()) }
    var runtime by remember { mutableStateOf((initial?.runtimeMinutes ?: 95).toString()) }
    var ageRating by remember { mutableStateOf(initial?.ageRating ?: "PG-13") }
    var director by remember { mutableStateOf(initial?.director ?: "Notflix Studios") }
    var castText by remember { mutableStateOf(initial?.cast?.joinToString(", ") ?: "") }
    var posterUrl by remember { mutableStateOf(initial?.posterUrl ?: "") }
    var backdropUrl by remember { mutableStateOf(initial?.backdropUrl ?: "") }
    var videoUrl by remember { mutableStateOf(initial?.videoUrl ?: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4") }
    var hlsUrl by remember { mutableStateOf(initial?.hlsUrl ?: "") }
    var trailerUrl by remember { mutableStateOf(initial?.trailerUrl ?: "") }
    var isFeatured by remember { mutableStateOf(initial?.isFeatured ?: false) }
    var isVisible by remember { mutableStateOf(initial?.isVisible ?: true) }
    var isKidsFriendly by remember { mutableStateOf(initial?.isKidsFriendly ?: false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VeloraSurfaceElevated,
        title = { Text(if (initial == null) "Add Title" else "Edit Title", color = VeloraOnBackground) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = mediaType == "movie", onClick = { mediaType = "movie" }, label = { Text("Movie") })
                    FilterChip(selected = mediaType == "series", onClick = { mediaType = "series" }, label = { Text("TV Show") })
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title*") }, singleLine = true)
                OutlinedTextField(value = synopsis, onValueChange = { synopsis = it }, label = { Text("Synopsis*") }, minLines = 2)
                OutlinedTextField(value = genresText, onValueChange = { genresText = it }, label = { Text("Genres (comma-separated)") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = releaseYear, onValueChange = { releaseYear = it }, label = { Text("Year") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = runtime, onValueChange = { runtime = it }, label = { Text("Mins") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = ageRating, onValueChange = { ageRating = it }, label = { Text("Rating") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = director, onValueChange = { director = it }, label = { Text("Director") }, singleLine = true)
                OutlinedTextField(value = castText, onValueChange = { castText = it }, label = { Text("Cast (comma-separated)") }, singleLine = true)
                OutlinedTextField(value = videoUrl, onValueChange = { videoUrl = it }, label = { Text("MP4 Video URL*") }, singleLine = true)
                OutlinedTextField(value = hlsUrl, onValueChange = { hlsUrl = it }, label = { Text("HLS (.m3u8) URL (Optional)") }, singleLine = true)
                OutlinedTextField(value = trailerUrl, onValueChange = { trailerUrl = it }, label = { Text("Trailer URL (Optional)") }, singleLine = true)
                OutlinedTextField(value = posterUrl, onValueChange = { posterUrl = it }, label = { Text("Poster Image URL (Optional)") }, singleLine = true)
                OutlinedTextField(value = backdropUrl, onValueChange = { backdropUrl = it }, label = { Text("Backdrop Image URL (Optional)") }, singleLine = true)

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Featured on Home", color = VeloraOnBackground)
                    Switch(checked = isFeatured, onCheckedChange = { isFeatured = it })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Visible to Users", color = VeloraOnBackground)
                    Switch(checked = isVisible, onCheckedChange = { isVisible = it })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Kids Friendly", color = VeloraOnBackground)
                    Switch(checked = isKidsFriendly, onCheckedChange = { isKidsFriendly = it })
                }

                errorMsg?.let { Text(it, color = VeloraError, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank() || synopsis.isBlank() || videoUrl.isBlank()) {
                        errorMsg = "Title, synopsis, and video URL are required"
                    } else {
                        onSave(
                            (initial ?: CatalogItem()).copy(
                                title = title.trim(),
                                synopsis = synopsis.trim(),
                                mediaType = mediaType,
                                genres = genresText.split(",").map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { listOf("Drama") },
                                releaseYear = releaseYear.toIntOrNull() ?: 2026,
                                runtimeMinutes = runtime.toIntOrNull() ?: 95,
                                ageRating = ageRating.ifBlank { "PG-13" },
                                director = director.ifBlank { "Notflix Studios" },
                                cast = castText.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                posterUrl = posterUrl.takeIf { it.isNotBlank() },
                                backdropUrl = backdropUrl.takeIf { it.isNotBlank() },
                                videoUrl = videoUrl.trim(),
                                hlsUrl = hlsUrl.takeIf { it.isNotBlank() },
                                trailerUrl = trailerUrl.takeIf { it.isNotBlank() },
                                isFeatured = isFeatured,
                                isVisible = isVisible,
                                isKidsFriendly = isKidsFriendly
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson)
            ) { Text("Save Title") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = VeloraOnSurfaceMuted) }
        }
    )
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
    var epVideoUrl by remember { mutableStateOf("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VeloraSurfaceElevated,
        title = { Text("Manage ${series.title}", color = VeloraOnBackground) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Create Season", style = MaterialTheme.typography.titleSmall, color = VeloraGold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = seasonNum, onValueChange = { seasonNum = it }, label = { Text("#") }, modifier = Modifier.width(68.dp), singleLine = true)
                    OutlinedTextField(value = seasonTitle, onValueChange = { seasonTitle = it }, label = { Text("Season Title") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedButton(onClick = {
                    val sNum = seasonNum.toIntOrNull() ?: 1
                    onAddSeason(Season(id = "season-$sNum", seasonNumber = sNum, title = seasonTitle, year = series.releaseYear, isVisible = true))
                }) { Text("Save Season", color = VeloraOnBackground) }

                Spacer(Modifier.height(6.dp))
                Text("Add Episode to Season $seasonNum", style = MaterialTheme.typography.titleSmall, color = VeloraGold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = epNum, onValueChange = { epNum = it }, label = { Text("Ep #") }, modifier = Modifier.width(76.dp), singleLine = true)
                    OutlinedTextField(value = epRuntime, onValueChange = { epRuntime = it }, label = { Text("Mins") }, modifier = Modifier.width(86.dp), singleLine = true)
                }
                OutlinedTextField(value = epTitle, onValueChange = { epTitle = it }, label = { Text("Episode Title") }, singleLine = true)
                OutlinedTextField(value = epSynopsis, onValueChange = { epSynopsis = it }, label = { Text("Episode Synopsis") }, minLines = 2)
                OutlinedTextField(value = epVideoUrl, onValueChange = { epVideoUrl = it }, label = { Text("Episode Stream URL") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sNum = seasonNum.toIntOrNull() ?: 1
                    val eNum = epNum.toIntOrNull() ?: 1
                    if (epTitle.isNotBlank() && epVideoUrl.isNotBlank()) {
                        onAddEpisode(
                            "season-$sNum",
                            Episode(
                                seasonNumber = sNum,
                                episodeNumber = eNum,
                                title = epTitle.trim(),
                                synopsis = epSynopsis.ifBlank { "Episode $eNum" },
                                runtimeMinutes = epRuntime.toIntOrNull() ?: 42,
                                thumbnailKey = series.backdropKey,
                                videoUrl = epVideoUrl.trim(),
                                isVisible = true
                            )
                        )
                        onDismiss()
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
