package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.Genre
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.VeloraAvatarPalettes
import com.example.ui.components.VeloraLogo
import com.example.ui.theme.VeloraBackground
import com.example.ui.theme.VeloraCrimson
import com.example.ui.theme.VeloraError
import com.example.ui.theme.VeloraGold
import com.example.ui.theme.VeloraOnBackground
import com.example.ui.theme.VeloraOnSurfaceMuted
import com.example.ui.theme.VeloraOutline
import com.example.ui.theme.VeloraSurfaceBright
import com.example.ui.theme.VeloraSurfaceElevated

@Composable
fun ProfileSelectorScreen(
    profilesState: UiState<List<UserProfile>>,
    genres: List<Genre>,
    isAdmin: Boolean = false,
    accountIdentifier: String? = null,
    onSelectProfile: (UserProfile) -> Unit,
    onOpenAdminDashboard: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onCreateProfile: (name: String, avatarKey: String, isKids: Boolean, genres: List<String>) -> Unit,
    onUpdateProfile: (profileId: String, name: String, avatarKey: String, isKids: Boolean, genres: List<String>, language: String, subtitles: Boolean) -> Unit,
    onDeleteProfile: (String) -> Unit
) {
    var editMode by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<UserProfile?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF14101A), VeloraBackground)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                VeloraLogo()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isAdmin) {
                        OutlinedButton(
                            onClick = onOpenAdminDashboard,
                            border = BorderStroke(1.dp, VeloraGold.copy(alpha = 0.65f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = VeloraGold.copy(alpha = 0.12f),
                                contentColor = VeloraGold
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("profile_selector_admin_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AdminPanelSettings,
                                contentDescription = "Admin Dashboard",
                                modifier = Modifier.size(16.dp),
                                tint = VeloraGold
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Admin", color = VeloraGold, maxLines = 1)
                        }
                    }
                    TextButton(
                        onClick = { editMode = !editMode },
                        modifier = Modifier.testTag("toggle_edit_profiles_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (editMode) VeloraCrimson else VeloraOnBackground
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (editMode) "Done" else "Manage",
                            color = if (editMode) VeloraCrimson else VeloraOnBackground
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = if (editMode) "Manage Profiles" else "Who's Watching?",
                style = MaterialTheme.typography.displayMedium,
                color = VeloraOnBackground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Each profile keeps its own watchlist, watch history, and recommendations.",
                style = MaterialTheme.typography.bodyMedium,
                color = VeloraOnSurfaceMuted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            when (profilesState) {
                is UiState.Loading -> {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = VeloraCrimson)
                    }
                }
                is UiState.Error -> {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(profilesState.message, color = VeloraError)
                    }
                }
                is UiState.Success -> {
                    val profiles = profilesState.data
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 136.dp),
                        modifier = Modifier
                            .widthIn(max = 640.dp)
                            .weight(1f),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        items(profiles, key = { it.id }) { profile ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        if (editMode) {
                                            editingProfile = profile
                                        } else {
                                            onSelectProfile(profile)
                                        }
                                    }
                                    .padding(8.dp)
                                    .testTag("profile_tile_${profile.id}")
                            ) {
                                Box {
                                    ProfileAvatar(
                                        avatarKey = profile.avatarKey,
                                        name = profile.name,
                                        isKids = profile.isKids,
                                        size = 96.dp
                                    )
                                    if (editMode) {
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .clip(RoundedCornerShape(21.dp))
                                                .background(Color.Black.copy(alpha = 0.55f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Edit,
                                                contentDescription = "Edit ${profile.name}",
                                                tint = Color.White,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = profile.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = VeloraOnBackground,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        if (profiles.size < 5) {
                            item {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { showCreateDialog = true }
                                        .padding(8.dp)
                                        .testTag("add_profile_button")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(96.dp)
                                            .clip(RoundedCornerShape(21.dp))
                                            .background(VeloraSurfaceElevated)
                                            .border(1.5.dp, VeloraOutline, RoundedCornerShape(21.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = "Add Profile",
                                            tint = VeloraOnBackground,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = "Add Profile",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = VeloraOnSurfaceMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = accountIdentifier?.takeIf { it.isNotBlank() }?.let { "Signed in as $it" } ?: "Notflix Account",
                    style = MaterialTheme.typography.bodySmall,
                    color = VeloraOnSurfaceMuted
                )
                TextButton(
                    onClick = onSignOut,
                    modifier = Modifier.testTag("profile_selector_sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = VeloraError,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Sign Out", color = VeloraError)
                }
            }
        }
    }

    if (showCreateDialog) {
        ProfileEditorDialog(
            initialProfile = null,
            genres = genres,
            canDelete = false,
            onDismiss = { showCreateDialog = false },
            onSave = { name, avatarKey, isKids, selectedGenres, _, _ ->
                onCreateProfile(name, avatarKey, isKids, selectedGenres)
                showCreateDialog = false
            },
            onDelete = {}
        )
    }

    editingProfile?.let { profile ->
        val totalProfiles = (profilesState as? UiState.Success)?.data?.size ?: 1
        ProfileEditorDialog(
            initialProfile = profile,
            genres = genres,
            canDelete = totalProfiles > 1,
            onDismiss = { editingProfile = null },
            onSave = { name, avatarKey, isKids, selectedGenres, language, subs ->
                onUpdateProfile(profile.id, name, avatarKey, isKids, selectedGenres, language, subs)
                editingProfile = null
            },
            onDelete = {
                onDeleteProfile(profile.id)
                editingProfile = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileEditorDialog(
    initialProfile: UserProfile?,
    genres: List<Genre>,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, avatarKey: String, isKids: Boolean, genres: List<String>, language: String, subtitles: Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(initialProfile?.name ?: "") }
    var avatarKey by remember { mutableStateOf(initialProfile?.avatarKey ?: "crimson") }
    var isKids by remember { mutableStateOf(initialProfile?.isKids ?: false) }
    var selectedGenres by remember { mutableStateOf(initialProfile?.preferredGenres?.toSet() ?: setOf("Sci-Fi", "Adventure")) }
    var subtitlesEnabled by remember { mutableStateOf(initialProfile?.subtitlesEnabled ?: true) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VeloraSurfaceElevated,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialProfile == null) "Create Profile" else "Edit Profile",
                    color = VeloraOnBackground
                )
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete profile",
                            tint = VeloraError
                        )
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        validationError = null
                    },
                    label = { Text("Profile Name") },
                    singleLine = true,
                    isError = validationError != null,
                    supportingText = validationError?.let { { Text(it, color = VeloraError) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input")
                )

                Text("Avatar Style", style = MaterialTheme.typography.labelLarge, color = VeloraOnBackground)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VeloraAvatarPalettes.forEach { palette ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (avatarKey == palette.key) 2.5.dp else 1.dp,
                                    color = if (avatarKey == palette.key) Color.White else VeloraOutline,
                                    shape = CircleShape
                                )
                                .clickable { avatarKey = palette.key },
                            contentAlignment = Alignment.Center
                        ) {
                            ProfileAvatar(
                                avatarKey = palette.key,
                                name = name.ifBlank { "V" },
                                isKids = false,
                                size = 40.dp
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Kids Profile", style = MaterialTheme.typography.titleSmall, color = VeloraOnBackground)
                        Text("Shows G / PG / TV-PG family-friendly titles only", style = MaterialTheme.typography.bodySmall, color = VeloraOnSurfaceMuted)
                    }
                    Switch(
                        checked = isKids,
                        onCheckedChange = { isKids = it },
                        modifier = Modifier.testTag("kids_profile_switch")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Subtitles Enabled", style = MaterialTheme.typography.titleSmall, color = VeloraOnBackground)
                    Switch(checked = subtitlesEnabled, onCheckedChange = { subtitlesEnabled = it })
                }

                Text("Favorite Genres", style = MaterialTheme.typography.labelLarge, color = VeloraOnBackground)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    genres.take(8).forEach { genre ->
                        val selected = genre.name in selectedGenres
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedGenres = if (selected) selectedGenres - genre.name else selectedGenres + genre.name
                            },
                            label = { Text(genre.name, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VeloraCrimson,
                                selectedLabelColor = Color.White,
                                containerColor = VeloraSurfaceBright,
                                labelColor = VeloraOnSurfaceMuted
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        validationError = "Please enter a profile name"
                    } else {
                        onSave(
                            trimmed,
                            avatarKey,
                            isKids,
                            selectedGenres.toList(),
                            initialProfile?.language ?: "English",
                            subtitlesEnabled
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson),
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, VeloraOutline)
            ) {
                Text("Cancel", color = VeloraOnBackground)
            }
        }
    )
}
