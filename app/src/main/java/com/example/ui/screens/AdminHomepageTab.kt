package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CatalogItem
import com.example.data.model.Genre
import com.example.data.model.HomepageRowRule
import com.example.data.model.HomepageSettings
import com.example.ui.theme.VeloraCoral
import com.example.ui.theme.VeloraCrimson
import com.example.ui.theme.VeloraGold
import com.example.ui.theme.VeloraMatchGreen
import com.example.ui.theme.VeloraOnBackground
import com.example.ui.theme.VeloraOnSurfaceMuted
import com.example.ui.theme.VeloraOutline
import com.example.ui.theme.VeloraSurfaceBright
import com.example.ui.theme.VeloraSurfaceElevated

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminHomepageTab(
    catalog: List<CatalogItem>,
    genres: List<Genre>,
    homepageSettings: HomepageSettings,
    homepageRowRules: List<HomepageRowRule>,
    onSaveHomepageSettings: (HomepageSettings) -> Unit,
    onSaveHomepageRowRules: (List<HomepageRowRule>) -> Unit,
    onResetHomepageLayout: () -> Unit,
    onSaveCatalogItem: (CatalogItem, () -> Unit) -> Unit,
    onAddComingSoonTitle: () -> Unit
) {
    var draftMaxItems by remember(homepageSettings) { mutableIntStateOf(homepageSettings.maxItemsPerRow) }
    var draftMaxBecause by remember(homepageSettings) { mutableIntStateOf(homepageSettings.maxBecauseYouWatchedRows) }
    var draftEnabledGenres by remember(homepageSettings) { mutableStateOf(homepageSettings.enabledGenres) }

    var draftRules by remember(homepageRowRules) {
        mutableStateOf(
            homepageRowRules.sortedBy { it.displayOrder }
                .mapIndexed { idx, r -> r.copy(displayOrder = idx) }
        )
    }
    var editingRule by remember { mutableStateOf<HomepageRowRule?>(null) }
    var showCuratedPicksDialog by remember { mutableStateOf(false) }

    val availableGenreNames = remember(genres) {
        val defaults = listOf(
            "Action", "Adventure", "Animation", "Anime", "Comedy",
            "Documentary", "Drama", "Fantasy", "Horror", "Mystery",
            "Romance", "Sci-Fi", "Thriller"
        )
        (genres.map { it.name } + defaults).distinctBy { it.lowercase() }.sorted()
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick Curation Actions (Featured, Staff Picks, Top 10, Coming Soon)
        item {
            Surface(
                color = VeloraSurfaceElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, VeloraOutline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Spotlight, Staff Picks & Coming Soon",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = VeloraGold
                    )
                    Text(
                        text = "Featured Hero: ${catalog.count { it.isFeatured && !it.isComingSoon }} • Staff Picks: ${catalog.count { it.isStaffPick }} • Top 10 Eligible: ${catalog.count { it.popularityRank <= 10 && !it.isComingSoon }} • Coming Soon: ${catalog.count { it.isComingSoon }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = VeloraOnSurfaceMuted
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onAddComingSoonTitle,
                            colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_add_coming_soon_button")
                        ) {
                            Icon(Icons.Filled.EventAvailable, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add Coming Soon")
                        }
                        OutlinedButton(
                            onClick = { showCuratedPicksDialog = true },
                            border = BorderStroke(1.dp, VeloraGold.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_manage_picks_button")
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = VeloraGold, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Curate Titles", color = VeloraGold)
                        }
                    }
                }
            }
        }

        // Global Limits & Genre Selection
        item {
            Surface(
                color = VeloraSurfaceElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, VeloraOutline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Homepage Discovery Limits & Genres",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = VeloraGold
                    )

                    StepperRow(
                        label = "Max Titles Per Row",
                        subtitle = "Upper cap across all homepage carousels",
                        value = draftMaxItems,
                        range = 3..25,
                        onValueChange = { draftMaxItems = it }
                    )

                    HorizontalDivider(color = VeloraOutline)

                    StepperRow(
                        label = "Max \"Because You Watched\" Rows",
                        subtitle = "Maximum finished-title recommendation rows shown",
                        value = draftMaxBecause,
                        range = 0..10,
                        onValueChange = { draftMaxBecause = it }
                    )

                    HorizontalDivider(color = VeloraOutline)

                    Text(
                        text = "Enabled Genre Rows (leave empty to allow all active genres)",
                        style = MaterialTheme.typography.labelLarge,
                        color = VeloraOnBackground
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableGenreNames.forEach { genreName ->
                            val selected = draftEnabledGenres.any { it.equals(genreName, ignoreCase = true) }
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    draftEnabledGenres = if (selected) {
                                        draftEnabledGenres.filterNot { it.equals(genreName, ignoreCase = true) }
                                    } else {
                                        draftEnabledGenres + genreName
                                    }
                                },
                                label = { Text(genreName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VeloraCrimson,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onSaveHomepageSettings(
                                    homepageSettings.copy(
                                        maxItemsPerRow = draftMaxItems,
                                        maxBecauseYouWatchedRows = draftMaxBecause,
                                        enabledGenres = draftEnabledGenres
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_save_home_settings")
                        ) {
                            Text("Save Global Settings")
                        }
                        if (draftEnabledGenres.isNotEmpty()) {
                            TextButton(onClick = { draftEnabledGenres = emptyList() }) {
                                Text("Allow All", color = VeloraOnSurfaceMuted)
                            }
                        }
                    }
                }
            }
        }

        // Row Layout Header + Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Homepage Rows (${draftRules.count { it.enabled }}/${draftRules.size} enabled)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = VeloraOnBackground
                    )
                    Text(
                        text = "Reorder rows, edit titles (use {profile} for active profile name), or toggle visibility.",
                        style = MaterialTheme.typography.bodySmall,
                        color = VeloraOnSurfaceMuted
                    )
                }
                TextButton(
                    onClick = onResetHomepageLayout,
                    modifier = Modifier.testTag("admin_reset_home_layout")
                ) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = null, tint = VeloraGold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reset", color = VeloraGold)
                }
            }
        }

        item {
            Button(
                onClick = {
                    val normalized = draftRules.mapIndexed { idx, r -> r.copy(displayOrder = idx) }
                    onSaveHomepageRowRules(normalized)
                },
                colors = ButtonDefaults.buttonColors(containerColor = VeloraMatchGreen, contentColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_save_row_layout")
            ) {
                Text("Save Row Order & Visibility", fontWeight = FontWeight.Bold)
            }
        }

        itemsIndexed(draftRules, key = { idx, r -> "${r.rowKey}_$idx" }) { index, rule ->
            Surface(
                color = VeloraSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (rule.enabled) VeloraOutline else VeloraSurfaceBright)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "${index + 1}. ${rule.title}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (rule.enabled) VeloraOnBackground else VeloraOnSurfaceMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (rule.personalized) VeloraCoral.copy(alpha = 0.16f) else VeloraSurfaceBright,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (rule.personalized) "PROFILE" else "GLOBAL",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (rule.personalized) VeloraCoral else VeloraOnSurfaceMuted,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                            Text(
                                text = "Max ${rule.maxItems} • ${rule.rowKey}",
                                style = MaterialTheme.typography.labelSmall,
                                color = VeloraOnSurfaceMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (index > 0) {
                                    val mutable = draftRules.toMutableList()
                                    val moved = mutable.removeAt(index)
                                    mutable.add(index - 1, moved)
                                    draftRules = mutable.mapIndexed { idx, r -> r.copy(displayOrder = idx) }
                                }
                            },
                            enabled = index > 0
                        ) {
                            Icon(
                                Icons.Filled.ArrowUpward,
                                contentDescription = "Move row up",
                                tint = if (index > 0) VeloraOnBackground else VeloraOnSurfaceMuted.copy(alpha = 0.35f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                if (index < draftRules.lastIndex) {
                                    val mutable = draftRules.toMutableList()
                                    val moved = mutable.removeAt(index)
                                    mutable.add(index + 1, moved)
                                    draftRules = mutable.mapIndexed { idx, r -> r.copy(displayOrder = idx) }
                                }
                            },
                            enabled = index < draftRules.lastIndex
                        ) {
                            Icon(
                                Icons.Filled.ArrowDownward,
                                contentDescription = "Move row down",
                                tint = if (index < draftRules.lastIndex) VeloraOnBackground else VeloraOnSurfaceMuted.copy(alpha = 0.35f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = { editingRule = rule }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit row", tint = VeloraGold, modifier = Modifier.size(18.dp))
                        }
                        Switch(
                            checked = rule.enabled,
                            onCheckedChange = { checked ->
                                draftRules = draftRules.map {
                                    if (it.rowKey == rule.rowKey) it.copy(enabled = checked) else it
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    editingRule?.let { target ->
        var titleInput by remember(target.rowKey) { mutableStateOf(target.title) }
        var maxItemsInput by remember(target.rowKey) { mutableStateOf(target.maxItems.toString()) }
        var personalizedInput by remember(target.rowKey) { mutableStateOf(target.personalized) }

        AlertDialog(
            onDismissRequest = { editingRule = null },
            containerColor = VeloraSurfaceElevated,
            title = { Text("Edit Row Rule", color = VeloraOnBackground) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it.take(120) },
                        label = { Text("Row Title") },
                        supportingText = { Text("Use {profile} to insert active profile name", color = VeloraOnSurfaceMuted) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = maxItemsInput,
                        onValueChange = { maxItemsInput = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Max Titles in Row (1–30)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Personalized Row", color = VeloraOnBackground)
                            Text("History & recommendations always remain strictly isolated per profile", style = MaterialTheme.typography.bodySmall, color = VeloraOnSurfaceMuted)
                        }
                        Switch(checked = personalizedInput, onCheckedChange = { personalizedInput = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updatedMax = (maxItemsInput.toIntOrNull() ?: target.maxItems).coerceIn(1, 30)
                        val updatedTitle = titleInput.trim().ifBlank { target.title }
                        draftRules = draftRules.map {
                            if (it.rowKey == target.rowKey) {
                                it.copy(title = updatedTitle, maxItems = updatedMax, personalized = personalizedInput)
                            } else it
                        }
                        editingRule = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson)
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { editingRule = null }) {
                    Text("Cancel", color = VeloraOnSurfaceMuted)
                }
            }
        )
    }

    if (showCuratedPicksDialog) {
        AlertDialog(
            onDismissRequest = { showCuratedPicksDialog = false },
            containerColor = VeloraSurfaceElevated,
            title = { Text("Curate Hero, Staff Picks & Top 10", color = VeloraOnBackground) },
            text = {
                Column(
                    modifier = Modifier
                        .height(380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    catalog.forEach { item ->
                        Surface(
                            color = VeloraSurfaceBright,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, VeloraOutline)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${item.title} (Rank #${item.popularityRank})",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = VeloraOnBackground
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    FilterChip(
                                        selected = item.isFeatured,
                                        enabled = !item.isComingSoon,
                                        onClick = { onSaveCatalogItem(item.copy(isFeatured = !item.isFeatured)) {} },
                                        leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(14.dp)) },
                                        label = { Text("Hero") }
                                    )
                                    FilterChip(
                                        selected = item.isStaffPick,
                                        onClick = { onSaveCatalogItem(item.copy(isStaffPick = !item.isStaffPick)) {} },
                                        label = { Text("Staff Pick") }
                                    )
                                    FilterChip(
                                        selected = item.popularityRank <= 10,
                                        onClick = {
                                            val newRank = if (item.popularityRank <= 10) 15 else 1
                                            onSaveCatalogItem(item.copy(popularityRank = newRank)) {}
                                        },
                                        label = { Text(if (item.popularityRank <= 10) "In Top 10" else "Add Top 10") }
                                    )
                                    FilterChip(
                                        selected = item.isComingSoon,
                                        onClick = {
                                            val nextComingSoon = !item.isComingSoon
                                            onSaveCatalogItem(
                                                item.copy(
                                                    isComingSoon = nextComingSoon,
                                                    isFeatured = if (nextComingSoon) false else item.isFeatured
                                                )
                                            ) {}
                                        },
                                        label = { Text("Coming Soon") }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showCuratedPicksDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson)
                ) { Text("Done") }
            }
        )
    }
}

@Composable
private fun StepperRow(
    label: String,
    subtitle: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = VeloraOnBackground)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = VeloraOnSurfaceMuted)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { if (value > range.first) onValueChange(value - 1) },
                enabled = value > range.first,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.size(36.dp)
            ) { Text("-") }
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = VeloraGold
            )
            OutlinedButton(
                onClick = { if (value < range.last) onValueChange(value + 1) },
                enabled = value < range.last,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.size(36.dp)
            ) { Text("+") }
        }
    }
}
