package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CatalogItem
import com.example.data.model.ContentRating
import com.example.data.model.Episode
import com.example.data.model.Genre
import com.example.data.model.HomepageRowRule
import com.example.data.model.HomepageSettings
import com.example.data.model.PlaybackProgress
import com.example.data.model.SearchHistoryItem
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.CatalogImage
import com.example.ui.components.PosterCard
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.QuickPreviewBottomSheet
import com.example.ui.components.TopTenPosterCard
import com.example.ui.components.VeloraLogo
import com.example.ui.state.ContinueWatchingEntry
import com.example.ui.state.HomeCategoryFilter
import com.example.ui.state.HomeFeedRowBuilder
import com.example.ui.state.HomeFeedRowType
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
import com.example.ui.theme.VeloraSurfaceBright
import com.example.ui.theme.VeloraSurfaceElevated

@Composable
fun HomeScreen(
    catalogState: UiState<List<CatalogItem>>,
    catalog: List<CatalogItem>,
    recommendations: List<CatalogItem>,
    continueWatching: List<ContinueWatchingEntry>,
    watchlistIds: Set<String>,
    genres: List<Genre>,
    activeProfile: UserProfile?,
    selectedCategory: HomeCategoryFilter,
    selectedGenre: String?,
    unreadNotifications: Int,
    isAdmin: Boolean = false,
    accountIdentifier: String? = null,
    progressList: List<PlaybackProgress> = emptyList(),
    ratings: List<ContentRating> = emptyList(),
    searchHistory: List<SearchHistoryItem> = emptyList(),
    homepageSettings: HomepageSettings = HomepageSettings(),
    homepageRowRules: List<HomepageRowRule> = HomeFeedRowBuilder.defaultRowRules(),
    seriesEpisodesById: Map<String, List<Episode>> = emptyMap(),
    onSelectCategory: (HomeCategoryFilter) -> Unit,
    onSelectGenre: (String?) -> Unit,
    onSwitchProfileClick: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenAdminDashboard: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onNotificationsClick: () -> Unit,
    onOpenDetails: (CatalogItem) -> Unit,
    onPlayItem: (CatalogItem, ContinueWatchingEntry?) -> Unit,
    onPlayNextEpisode: (CatalogItem, Episode) -> Unit = { item, _ -> onPlayItem(item, null) },
    onPlayTrailer: (CatalogItem) -> Unit = { onOpenDetails(it) },
    onToggleWatchlist: (CatalogItem) -> Unit
) {
    var previewItem by remember { mutableStateOf<CatalogItem?>(null) }
    var heroMuted by remember { mutableStateOf(true) }
    var heroIndex by remember { mutableIntStateOf(0) }
    var showAccountMenu by remember { mutableStateOf(false) }

    val safeProfileName = remember(activeProfile?.name) {
        activeProfile?.name
            ?.trim()
            ?.takeIf { it.isNotBlank() && !it.contains('@') }
            ?: "Viewer"
    }

    val filteredCatalog = remember(catalog, selectedCategory, selectedGenre) {
        catalog.filter { item ->
            val matchesCategory = when (selectedCategory) {
                HomeCategoryFilter.ALL -> true
                HomeCategoryFilter.MOVIES -> item.mediaType == "movie"
                HomeCategoryFilter.SERIES -> item.mediaType == "series"
                HomeCategoryFilter.NEW_POPULAR -> item.releaseYear >= 2025 || item.trendingScore >= 900
                HomeCategoryFilter.KIDS -> item.isKidsFriendly
            }
            val matchesGenre = selectedGenre == null || item.genres.any { it.equals(selectedGenre, ignoreCase = true) }
            matchesCategory && matchesGenre
        }
    }

    val featuredItems = remember(filteredCatalog) {
        val playable = filteredCatalog.filter { it.isPlayableNow }
        playable.filter { it.isFeatured }.ifEmpty { playable.take(3) }
    }
    val heroItem = featuredItems.getOrNull(heroIndex % featuredItems.size.coerceAtLeast(1))

    val feedRows = remember(
        filteredCatalog,
        catalog,
        recommendations,
        continueWatching,
        watchlistIds,
        progressList,
        ratings,
        searchHistory,
        genres,
        activeProfile,
        selectedCategory,
        selectedGenre,
        homepageSettings,
        homepageRowRules,
        seriesEpisodesById
    ) {
        HomeFeedRowBuilder.buildRows(
            filteredCatalog = filteredCatalog,
            fullVisibleCatalog = catalog,
            recommendations = recommendations,
            continueWatching = continueWatching,
            watchlistIds = watchlistIds,
            progressList = progressList,
            ratings = ratings,
            searchHistory = searchHistory,
            genres = genres,
            activeProfile = activeProfile,
            selectedCategory = selectedCategory,
            selectedGenre = selectedGenre,
            homepageSettings = homepageSettings,
            homepageRowRules = homepageRowRules,
            seriesEpisodesById = seriesEpisodesById
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VeloraBackground)
    ) {
        when (catalogState) {
            is UiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VeloraCrimson)
                }
            }
            is UiState.Error -> {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(catalogState.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is UiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    // Top Bar + Filter Strip + Hero Banner
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(510.dp)
                        ) {
                            if (heroItem != null) {
                                CatalogImage(
                                    imageUrl = heroItem.backdropUrl,
                                    fallbackKey = heroItem.backdropKey,
                                    contentDescription = heroItem.title,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            0f to VeloraBackground.copy(alpha = 0.78f),
                                            0.3f to Color.Transparent,
                                            0.65f to VeloraBackground.copy(alpha = 0.65f),
                                            1f to VeloraBackground
                                        )
                                    )
                            )

                            // Top Overlay Header + Category Pills
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(top = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        VeloraLogo()
                                        if (activeProfile?.isKids == true) {
                                            Surface(
                                                color = VeloraKidsCyan,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "KIDS",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color.Black,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        IconButton(
                                            onClick = onNotificationsClick,
                                            modifier = Modifier.testTag("home_notifications_button")
                                        ) {
                                            BadgedBox(
                                                badge = {
                                                    if (unreadNotifications > 0) {
                                                        Badge(containerColor = VeloraCrimson) {
                                                            Text(unreadNotifications.toString(), color = Color.White)
                                                        }
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Notifications,
                                                    contentDescription = "Notifications",
                                                    tint = VeloraOnBackground
                                                )
                                            }
                                        }
                                        activeProfile?.let { profile ->
                                            Box {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(CircleShape)
                                                        .clickable { showAccountMenu = true }
                                                        .padding(4.dp)
                                                        .testTag("home_profile_switcher")
                                                ) {
                                                    ProfileAvatar(
                                                        avatarKey = profile.avatarKey,
                                                        name = profile.name,
                                                        isKids = profile.isKids,
                                                        size = 36.dp
                                                    )
                                                }
                                                DropdownMenu(
                                                    expanded = showAccountMenu,
                                                    onDismissRequest = { showAccountMenu = false },
                                                    containerColor = VeloraSurfaceElevated,
                                                    modifier = Modifier.testTag("home_account_profile_menu")
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Text(
                                                                text = profile.name,
                                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                                color = VeloraOnBackground
                                                            )
                                                            if (isAdmin) {
                                                                Surface(
                                                                    color = VeloraGold.copy(alpha = 0.2f),
                                                                    shape = RoundedCornerShape(4.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "ADMIN",
                                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                        color = VeloraGold,
                                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        accountIdentifier?.takeIf { it.isNotBlank() }?.let { idText ->
                                                            Text(
                                                                text = idText,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = VeloraOnSurfaceMuted,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                    HorizontalDivider(color = VeloraOutline)
                                                    if (isAdmin) {
                                                        DropdownMenuItem(
                                                            text = {
                                                                Text(
                                                                    "Admin Dashboard",
                                                                    color = VeloraGold,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            },
                                                            leadingIcon = {
                                                                Icon(
                                                                    Icons.Filled.AdminPanelSettings,
                                                                    contentDescription = null,
                                                                    tint = VeloraGold
                                                                )
                                                            },
                                                            onClick = {
                                                                showAccountMenu = false
                                                                onOpenAdminDashboard()
                                                            },
                                                            modifier = Modifier.testTag("profile_menu_admin_dashboard")
                                                        )
                                                        HorizontalDivider(color = VeloraOutline)
                                                    }
                                                    DropdownMenuItem(
                                                        text = { Text("Switch Profile", color = VeloraOnBackground) },
                                                        leadingIcon = {
                                                            Icon(
                                                                Icons.Filled.People,
                                                                contentDescription = null,
                                                                tint = VeloraOnBackground
                                                            )
                                                        },
                                                        onClick = {
                                                            showAccountMenu = false
                                                            onSwitchProfileClick()
                                                        },
                                                        modifier = Modifier.testTag("profile_menu_switch_profile")
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Account Details & Settings", color = VeloraOnBackground) },
                                                        leadingIcon = {
                                                            Icon(
                                                                Icons.Filled.Settings,
                                                                contentDescription = null,
                                                                tint = VeloraOnBackground
                                                            )
                                                        },
                                                        onClick = {
                                                            showAccountMenu = false
                                                            onOpenSettings()
                                                        },
                                                        modifier = Modifier.testTag("profile_menu_settings")
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Sign Out", color = VeloraError) },
                                                        leadingIcon = {
                                                            Icon(
                                                                Icons.AutoMirrored.Filled.Logout,
                                                                contentDescription = null,
                                                                tint = VeloraError
                                                            )
                                                        },
                                                        onClick = {
                                                            showAccountMenu = false
                                                            onSignOut()
                                                        },
                                                        modifier = Modifier.testTag("profile_menu_sign_out")
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Personalized Welcome Message (Active Profile Name Only)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (activeProfile?.isKids == true) {
                                            "Kids Spotlight for $safeProfileName"
                                        } else {
                                            "Welcome back, $safeProfileName"
                                        },
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = VeloraOnBackground.copy(alpha = 0.92f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("home_welcome_message")
                                    )
                                }

                                // Category Filter Chips
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(HomeCategoryFilter.entries) { cat ->
                                        val isSelected = selectedCategory == cat && selectedGenre == null
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                onSelectGenre(null)
                                                onSelectCategory(cat)
                                            },
                                            label = { Text(cat.label) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = VeloraCrimson,
                                                selectedLabelColor = Color.White,
                                                containerColor = Color.Black.copy(alpha = 0.55f),
                                                labelColor = VeloraOnBackground
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = isSelected,
                                                borderColor = Color.White.copy(alpha = 0.25f)
                                            )
                                        )
                                    }
                                    itemsIndexed(genres.take(6), key = { idx, genre -> "${genre.id.ifBlank { "genre" }}_$idx" }) { _, genre ->
                                        val isSelected = selectedGenre.equals(genre.name, ignoreCase = true)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { onSelectGenre(genre.name) },
                                            label = { Text(genre.name) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = VeloraCoral,
                                                selectedLabelColor = Color.White,
                                                containerColor = Color.Black.copy(alpha = 0.55f),
                                                labelColor = VeloraOnBackground
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true,
                                                selected = isSelected,
                                                borderColor = Color.White.copy(alpha = 0.25f)
                                            )
                                        )
                                    }
                                }
                            }

                            // Hero Metadata & Actions at Bottom
                            if (heroItem != null) {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = VeloraCrimson,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = if (heroItem.isSeries) "FEATURED SERIES" else "FEATURED FILM",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                )
                                            }
                                            Text(
                                                text = "${(heroItem.trendingScore / 10).coerceIn(85, 99)}% Match • ${heroItem.releaseYear} • ${heroItem.ageRating}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = VeloraMatchGreen
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = { heroMuted = !heroMuted },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(alpha = 0.5f))
                                            ) {
                                                Icon(
                                                    imageVector = if (heroMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                                    contentDescription = "Toggle preview audio",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = heroItem.title,
                                        style = MaterialTheme.typography.displayLarge,
                                        color = VeloraOnBackground,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = heroItem.genres.joinToString(" • ") + " • ${heroItem.runtimeMinutes}m",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = VeloraOnSurfaceMuted
                                    )
                                    Text(
                                        text = heroItem.synopsis,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = VeloraOnBackground.copy(alpha = 0.88f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val heroContinue = continueWatching.firstOrNull { it.catalogItem.id == heroItem.id }
                                        Button(
                                            onClick = { onPlayItem(heroItem, heroContinue) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .testTag("hero_play_button"),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                                        ) {
                                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = if (heroContinue != null) "Resume" else "Play",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }

                                        val inList = heroItem.id in watchlistIds
                                        OutlinedButton(
                                            onClick = { onToggleWatchlist(heroItem) },
                                            modifier = Modifier
                                                .height(48.dp)
                                                .testTag("hero_my_list_button"),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = Color.Black.copy(alpha = 0.45f),
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Icon(if (inList) Icons.Filled.Check else Icons.Filled.Add, contentDescription = null)
                                            Spacer(Modifier.width(4.dp))
                                            Text(if (inList) "Saved" else "My List")
                                        }

                                        IconButton(
                                            onClick = { onOpenDetails(heroItem) },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(VeloraSurfaceElevated.copy(alpha = 0.85f))
                                                .testTag("hero_more_info_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Info,
                                                contentDescription = "More Info",
                                                tint = Color.White
                                            )
                                        }
                                    }

                                    if (featuredItems.size > 1) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            featuredItems.forEachIndexed { idx, _ ->
                                                val active = idx == (heroIndex % featuredItems.size)
                                                Box(
                                                    modifier = Modifier
                                                        .height(4.dp)
                                                        .width(if (active) 24.dp else 10.dp)
                                                        .clip(CircleShape)
                                                        .background(if (active) VeloraCrimson else Color.White.copy(alpha = 0.35f))
                                                        .clickable { heroIndex = idx }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Dynamic Personalized & Category Feed Rows
                    feedRows.forEach { row ->
                        item(key = "home_row_${row.id}") {
                            when (row.type) {
                                HomeFeedRowType.CONTINUE_WATCHING -> {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.testTag("home_row_${row.id}")
                                    ) {
                                        SectionHeader(row.title, subtitle = row.subtitle)
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            itemsIndexed(
                                                row.continueEntries,
                                                key = { idx, entry ->
                                                    "cw_${entry.progress.id.ifBlank { entry.catalogItem.id }}_$idx"
                                                }
                                            ) { _, entry ->
                                                val epTag = if (entry.progress.seasonNumber != null && entry.progress.episodeNumber != null) {
                                                    "S${entry.progress.seasonNumber}:E${entry.progress.episodeNumber}"
                                                } else {
                                                    val minsLeft = ((entry.progress.durationMs - entry.progress.positionMs) / 60000L).coerceAtLeast(1L)
                                                    "${minsLeft}m left"
                                                }
                                                PosterCard(
                                                    item = entry.catalogItem,
                                                    progressFraction = entry.progress.progressFraction,
                                                    subtitleOverride = epTag,
                                                    onClick = { onPlayItem(entry.catalogItem, entry) },
                                                    onQuickPreview = { previewItem = entry.catalogItem }
                                                )
                                            }
                                        }
                                    }
                                }
                                HomeFeedRowType.NEXT_EPISODES -> {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.testTag("home_row_${row.id}")
                                    ) {
                                        SectionHeader(row.title, subtitle = row.subtitle)
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            itemsIndexed(
                                                row.nextEpisodeEntries,
                                                key = { idx, entry ->
                                                    "next_${entry.catalogItem.id}_${entry.nextEpisode.id}_$idx"
                                                }
                                            ) { _, entry ->
                                                PosterCard(
                                                    item = entry.catalogItem,
                                                    subtitleOverride = "S${entry.nextEpisode.seasonNumber}:E${entry.nextEpisode.episodeNumber} • Up Next",
                                                    onClick = { onPlayNextEpisode(entry.catalogItem, entry.nextEpisode) },
                                                    onQuickPreview = { previewItem = entry.catalogItem }
                                                )
                                            }
                                        }
                                    }
                                }
                                HomeFeedRowType.TOP_TEN -> {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.testTag("home_row_${row.id}")
                                    ) {
                                        SectionHeader(row.title, subtitle = row.subtitle)
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            itemsIndexed(
                                                row.items,
                                                key = { idx, item -> "${row.id}_${item.id}_$idx" }
                                            ) { index, item ->
                                                TopTenPosterCard(
                                                    rank = index + 1,
                                                    item = item,
                                                    onClick = { onOpenDetails(item) }
                                                )
                                            }
                                        }
                                    }
                                }
                                HomeFeedRowType.COMING_SOON -> {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.testTag("home_row_${row.id}")
                                    ) {
                                        SectionHeader(row.title, subtitle = row.subtitle)
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            itemsIndexed(
                                                row.items,
                                                key = { idx, item -> "${row.id}_${item.id}_$idx" }
                                            ) { _, item ->
                                                val dateLabel = item.expectedReleaseDate?.takeIf { it.isNotBlank() }
                                                    ?: item.releaseYear.toString()
                                                PosterCard(
                                                    item = item,
                                                    subtitleOverride = "Expected $dateLabel",
                                                    onClick = { onOpenDetails(item) },
                                                    onQuickPreview = { previewItem = item }
                                                )
                                            }
                                        }
                                    }
                                }
                                HomeFeedRowType.STANDARD_CAROUSEL -> {
                                    CatalogCarouselRow(
                                        rowId = row.id,
                                        title = row.title,
                                        subtitle = row.subtitle,
                                        items = row.items,
                                        subtitleBadge = row.badgeTag,
                                        onCardClick = onOpenDetails,
                                        onQuickPreview = { previewItem = it },
                                        modifier = Modifier.testTag("home_row_${row.id}")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    previewItem?.let { item ->
        QuickPreviewBottomSheet(
            item = item,
            inWatchlist = item.id in watchlistIds,
            onDismiss = { previewItem = null },
            onPlay = {
                previewItem = null
                if (item.isComingSoon) {
                    onOpenDetails(item)
                } else {
                    val entry = continueWatching.firstOrNull { it.catalogItem.id == item.id }
                    onPlayItem(item, entry)
                }
            },
            onPlayTrailer = if (!item.trailerUrl.isNullOrBlank()) {
                {
                    previewItem = null
                    onPlayTrailer(item)
                }
            } else null,
            onToggleWatchlist = { onToggleWatchlist(item) },
            onMoreInfo = {
                previewItem = null
                onOpenDetails(item)
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = VeloraOnBackground
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = VeloraOnSurfaceMuted
            )
        }
    }
}

@Composable
private fun CatalogCarouselRow(
    rowId: String,
    title: String,
    items: List<CatalogItem>,
    onCardClick: (CatalogItem) -> Unit,
    onQuickPreview: (CatalogItem) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleBadge: String? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionHeader(title, subtitle)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(items, key = { idx, item -> "${rowId}_${item.id}_$idx" }) { _, item ->
                PosterCard(
                    item = item,
                    subtitleOverride = subtitleBadge?.let { "$it • ${item.releaseYear}" },
                    onClick = { onCardClick(item) },
                    onQuickPreview = { onQuickPreview(item) }
                )
            }
        }
    }
}
