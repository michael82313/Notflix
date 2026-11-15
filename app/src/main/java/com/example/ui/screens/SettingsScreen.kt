package com.example.ui.screens

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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PlanFeature
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
    userSubscription: UserSubscription,
    freePlanFeatures: List<PlanFeature>,
    isAdmin: Boolean,
    onSwitchProfile: () -> Unit,
    onOpenAdminDashboard: () -> Unit,
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
    var displayName by remember(account?.displayName) { mutableStateOf(account?.displayName ?: "Notflix Viewer") }
    var language by remember(account?.languagePreference) { mutableStateOf(account?.languagePreference ?: "English") }
    var autoplayNext by remember(account?.autoplayNext) { mutableStateOf(account?.autoplayNext ?: true) }
    var quality by remember(account?.defaultQuality) { mutableStateOf(account?.defaultQuality ?: "Auto") }
    var subtitlesEnabled by remember(account?.subtitlesEnabled) { mutableStateOf(account?.subtitlesEnabled ?: true) }
    var subtitleSize by remember(account?.subtitleSize) { mutableStateOf(account?.subtitleSize ?: "Medium") }

    var showLanguageMenu by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var showSubSizeMenu by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(VeloraBackground)
            .statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Account & Settings",
                style = MaterialTheme.typography.headlineLarge,
                color = VeloraOnBackground
            )
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

        // Permanent Free Plan Status (no paid tiers or billing active)
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
                                text = "Permanent Free Tier • Never Expires • Full Access Included",
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
                }
            }
        }

        // Admin Dashboard Entry
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
                                Text("Admin Dashboard", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = VeloraOnBackground)
                                Text("Manage movies, TV series, seasons, episodes, genres & users", style = MaterialTheme.typography.bodySmall, color = VeloraOnSurfaceMuted)
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

                    // Language Picker
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

                    // Quality Picker
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

                    // Subtitle Size Picker
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
}
