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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CatalogItem
import com.example.data.model.PlaybackProgress
import com.example.data.model.UserNotification
import com.example.data.model.WatchlistItem
import com.example.ui.components.CatalogImage
import com.example.ui.components.PosterCard
import com.example.ui.components.QuickPreviewBottomSheet
import com.example.ui.theme.VeloraBackground
import com.example.ui.theme.VeloraCrimson
import com.example.ui.theme.VeloraError
import com.example.ui.theme.VeloraOnBackground
import com.example.ui.theme.VeloraOnSurfaceMuted
import com.example.ui.theme.VeloraOutline
import com.example.ui.theme.VeloraSurfaceBright
import com.example.ui.theme.VeloraSurfaceElevated

private enum class MyListSort(val label: String) {
    RECENT("Recently Added"),
    TITLE("Title A-Z"),
    YEAR("Release Year")
}

@Composable
fun MyListAndActivityScreen(
    initialTab: Int = 0,
    catalog: List<CatalogItem>,
    watchlist: List<WatchlistItem>,
    progressList: List<PlaybackProgress>,
    notifications: List<UserNotification>,
    onOpenDetails: (CatalogItem) -> Unit,
    onPlayProgress: (CatalogItem, PlaybackProgress) -> Unit,
    onToggleWatchlist: (CatalogItem) -> Unit,
    onDeleteHistoryEntry: (String) -> Unit,
    onClearHistory: () -> Unit,
    onMarkNotificationRead: (String) -> Unit
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var sortMode by remember { mutableStateOf(MyListSort.RECENT) }
    var showSortMenu by remember { mutableStateOf(false) }
    var previewItem by remember { mutableStateOf<CatalogItem?>(null) }

    val catalogById = remember(catalog) { catalog.associateBy { it.id } }
    val watchlistIds = remember(watchlist) { watchlist.map { it.contentId }.toSet() }

    val savedItems = remember(watchlist, catalogById, sortMode) {
        val list = watchlist.mapNotNull { catalogById[it.contentId] }
        when (sortMode) {
            MyListSort.RECENT -> list
            MyListSort.TITLE -> list.sortedBy { it.title }
            MyListSort.YEAR -> list.sortedByDescending { it.releaseYear }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VeloraBackground)
            .statusBarsPadding()
            .padding(top = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Library & Activity",
                style = MaterialTheme.typography.headlineLarge,
                color = VeloraOnBackground
            )
            if (selectedTab == 0 && savedItems.isNotEmpty()) {
                Box {
                    Surface(
                        color = VeloraSurfaceElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, VeloraOutline),
                        modifier = Modifier.clickable { showSortMenu = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, tint = VeloraOnBackground, modifier = Modifier.size(16.dp))
                            Text(sortMode.label, style = MaterialTheme.typography.labelMedium, color = VeloraOnBackground)
                        }
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        containerColor = VeloraSurfaceElevated
                    ) {
                        MyListSort.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.label, color = VeloraOnBackground) },
                                onClick = {
                                    sortMode = mode
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            } else if (selectedTab == 1 && progressList.isNotEmpty()) {
                TextButton(onClick = onClearHistory) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = VeloraError, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Clear All", color = VeloraError)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = VeloraBackground,
            contentColor = VeloraOnBackground,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = VeloraCrimson
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("My List (${savedItems.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("History (${progressList.size})") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Alerts (${notifications.count { !it.read }})") }
            )
        }

        when (selectedTab) {
            0 -> {
                if (savedItems.isEmpty()) {
                    EmptyTabState(
                        icon = Icons.Filled.BookmarkBorder,
                        title = "Your My List is empty",
                        description = "Save movies and TV shows from Home or Explore to watch them anytime across all your devices."
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 140.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(savedItems, key = { it.id }) { item ->
                            PosterCard(
                                item = item,
                                width = 160.dp,
                                onClick = { onOpenDetails(item) },
                                onQuickPreview = { previewItem = item }
                            )
                        }
                    }
                }
            }
            1 -> {
                if (progressList.isEmpty()) {
                    EmptyTabState(
                        icon = Icons.Filled.History,
                        title = "No watch history yet",
                        description = "Titles you start watching will appear here with exact resume timestamps."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(progressList, key = { it.id.ifBlank { it.contentId } }) { entry ->
                            val item = catalogById[entry.contentId]
                            Surface(
                                color = VeloraSurfaceElevated,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, VeloraOutline),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = item != null) {
                                        if (item != null) onPlayProgress(item, entry)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (item != null) {
                                        CatalogImage(
                                            imageUrl = item.backdropUrl,
                                            fallbackKey = item.backdropKey,
                                            contentDescription = entry.title,
                                            modifier = Modifier
                                                .width(104.dp)
                                                .aspectRatio(16f / 9f)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = entry.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = VeloraOnBackground,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val epLabel = if (entry.seasonNumber != null && entry.episodeNumber != null) {
                                            "S${entry.seasonNumber} E${entry.episodeNumber} • "
                                        } else ""
                                        Text(
                                            text = if (entry.completed) "${epLabel}Completed" else "${epLabel}${(entry.progressFraction * 100).toInt()}% watched",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = VeloraOnSurfaceMuted
                                        )
                                        LinearProgressIndicator(
                                            progress = { if (entry.completed) 1f else entry.progressFraction.coerceIn(0.03f, 1f) },
                                            color = VeloraCrimson,
                                            trackColor = VeloraSurfaceBright,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                        )
                                    }
                                    if (item != null) {
                                        IconButton(onClick = { onPlayProgress(item, entry) }) {
                                            Icon(Icons.Filled.PlayArrow, contentDescription = "Resume", tint = VeloraOnBackground)
                                        }
                                    }
                                    IconButton(onClick = { onDeleteHistoryEntry(entry.id) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Remove from history", tint = VeloraOnSurfaceMuted)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                if (notifications.isEmpty()) {
                    EmptyTabState(
                        icon = Icons.Filled.NotificationsNone,
                        title = "You're all caught up",
                        description = "New releases, series updates, and personalized picks will show up here."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(notifications, key = { it.id }) { notif ->
                            Surface(
                                color = if (notif.read) VeloraSurfaceElevated else VeloraSurfaceBright,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (notif.read) VeloraOutline else VeloraCrimson.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (!notif.read) onMarkNotificationRead(notif.id)
                                        notif.contentId?.let { cid -> catalogById[cid]?.let(onOpenDetails) }
                                    }
                                    .testTag("notification_card_${notif.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = notif.title,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = VeloraOnBackground
                                        )
                                        if (!notif.read) {
                                            Surface(color = VeloraCrimson, shape = RoundedCornerShape(4.dp)) {
                                                Text(
                                                    text = "NEW",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = notif.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = VeloraOnSurfaceMuted
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
                onOpenDetails(item)
            },
            onToggleWatchlist = { onToggleWatchlist(item) },
            onMoreInfo = {
                previewItem = null
                onOpenDetails(item)
            }
        )
    }
}

@Composable
private fun EmptyTabState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = VeloraOnSurfaceMuted, modifier = Modifier.size(52.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium, color = VeloraOnBackground)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = VeloraOnSurfaceMuted)
        }
    }
}
