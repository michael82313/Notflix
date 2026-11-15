package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.data.model.CatalogItem
import com.example.data.model.Genre
import com.example.data.model.SearchHistoryItem
import com.example.ui.components.PosterCard
import com.example.ui.components.QuickPreviewBottomSheet
import com.example.ui.state.SortOption
import com.example.ui.theme.VeloraBackground
import com.example.ui.theme.VeloraCrimson
import com.example.ui.theme.VeloraOnBackground
import com.example.ui.theme.VeloraOnSurfaceMuted
import com.example.ui.theme.VeloraOutline
import com.example.ui.theme.VeloraSurfaceBright
import com.example.ui.theme.VeloraSurfaceElevated

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExploreScreen(
    query: String,
    mediaFilter: String,
    genreFilter: String?,
    sortOption: SortOption,
    results: List<CatalogItem>,
    genres: List<Genre>,
    searchHistory: List<SearchHistoryItem>,
    watchlistIds: Set<String>,
    onQueryChange: (String) -> Unit,
    onSubmitQuery: (String) -> Unit,
    onDeleteHistory: (String) -> Unit,
    onMediaFilterChange: (String) -> Unit,
    onGenreFilterChange: (String?) -> Unit,
    onSortChange: (SortOption) -> Unit,
    onResetFilters: () -> Unit,
    onOpenDetails: (CatalogItem) -> Unit,
    onPlayItem: (CatalogItem) -> Unit,
    onToggleWatchlist: (CatalogItem) -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var previewItem by remember { mutableStateOf<CatalogItem?>(null) }

    val suggestions = remember(genres) {
        listOf("Tears of Steel", "Midnight Bridge", "Sci-Fi", "Ian Hubert", "Sintel", "Orbital") +
            genres.take(3).map { it.name }
    }.distinct()

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
                text = "Explore & Search",
                style = MaterialTheme.typography.headlineLarge,
                color = VeloraOnBackground
            )
            Box {
                Surface(
                    color = VeloraSurfaceElevated,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, VeloraOutline),
                    modifier = Modifier.clickable { showSortMenu = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = VeloraOnBackground, modifier = Modifier.size(18.dp))
                        Text(sortOption.label, style = MaterialTheme.typography.labelLarge, color = VeloraOnBackground)
                    }
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    containerColor = VeloraSurfaceElevated
                ) {
                    SortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label, color = VeloraOnBackground) },
                            onClick = {
                                onSortChange(option)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Movies, series, actors, directors, or genres...", color = VeloraOnSurfaceMuted) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = VeloraOnSurfaceMuted) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = VeloraOnSurfaceMuted)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmitQuery(query) }),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VeloraCrimson,
                unfocusedBorderColor = VeloraOutline,
                focusedContainerColor = VeloraSurfaceElevated,
                unfocusedContainerColor = VeloraSurfaceElevated,
                focusedTextColor = VeloraOnBackground,
                unfocusedTextColor = VeloraOnBackground
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("search_input")
        )

        // Media type + Genre chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val typeOptions = listOf("all" to "All Titles", "movie" to "Movies", "series" to "TV Shows")
            items(typeOptions) { (key, label) ->
                val selected = mediaFilter == key
                FilterChip(
                    selected = selected,
                    onClick = { onMediaFilterChange(key) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VeloraCrimson,
                        selectedLabelColor = Color.White,
                        containerColor = VeloraSurfaceElevated,
                        labelColor = VeloraOnSurfaceMuted
                    )
                )
            }
            items(genres, key = { it.id }) { genre ->
                val selected = genreFilter.equals(genre.name, ignoreCase = true)
                FilterChip(
                    selected = selected,
                    onClick = { onGenreFilterChange(genre.name) },
                    label = { Text(genre.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VeloraSurfaceBright,
                        selectedLabelColor = Color.White,
                        containerColor = VeloraSurfaceElevated,
                        labelColor = VeloraOnSurfaceMuted
                    )
                )
            }
        }

        // Search History & Suggestions when query is blank
        if (query.isBlank()) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (searchHistory.isNotEmpty()) {
                    Text("Recent Searches", style = MaterialTheme.typography.labelLarge, color = VeloraOnSurfaceMuted)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        searchHistory.take(6).forEach { entry ->
                            InputChip(
                                selected = false,
                                onClick = {
                                    onQueryChange(entry.query)
                                    onSubmitQuery(entry.query)
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                trailingIcon = {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Remove ${entry.query}",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { onDeleteHistory(entry.id) }
                                    )
                                },
                                label = { Text(entry.query) },
                                colors = InputChipDefaults.inputChipColors(
                                    containerColor = VeloraSurfaceElevated,
                                    labelColor = VeloraOnBackground
                                )
                            )
                        }
                    }
                }

                Text("Suggestions", style = MaterialTheme.typography.labelLarge, color = VeloraOnSurfaceMuted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(suggestions) { suggestion ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                onQueryChange(suggestion)
                                onSubmitQuery(suggestion)
                            },
                            label = { Text(suggestion) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = VeloraSurfaceElevated,
                                labelColor = VeloraOnBackground
                            )
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        if (results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SearchOff,
                        contentDescription = null,
                        tint = VeloraOnSurfaceMuted,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "No matching titles found",
                        style = MaterialTheme.typography.headlineMedium,
                        color = VeloraOnBackground
                    )
                    Text(
                        text = "Try searching for a different title, actor, director, or clear your filters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VeloraOnSurfaceMuted
                    )
                    Button(
                        onClick = onResetFilters,
                        colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson)
                    ) {
                        Text("Reset Filters")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(results, key = { it.id }) { item ->
                    PosterCard(
                        item = item,
                        width = 160.dp,
                        onClick = {
                            if (query.isNotBlank()) onSubmitQuery(query)
                            onOpenDetails(item)
                        },
                        onQuickPreview = { previewItem = item }
                    )
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
                onPlayItem(item)
            },
            onToggleWatchlist = { onToggleWatchlist(item) },
            onMoreInfo = {
                previewItem = null
                onOpenDetails(item)
            }
        )
    }
}
