package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.CatalogItem
import com.example.ui.theme.VeloraBackground
import com.example.ui.theme.VeloraCoral
import com.example.ui.theme.VeloraCrimson
import com.example.ui.theme.VeloraGold
import com.example.ui.theme.VeloraKidsCyan
import com.example.ui.theme.VeloraMatchGreen
import com.example.ui.theme.VeloraOnBackground
import com.example.ui.theme.VeloraOnSurfaceMuted
import com.example.ui.theme.VeloraOutline
import com.example.ui.theme.VeloraSurface
import com.example.ui.theme.VeloraSurfaceBright
import com.example.ui.theme.VeloraSurfaceElevated

@DrawableRes
fun resolveArtworkDrawable(key: String): Int {
    return when (key) {
        "img_hero_nebula" -> R.drawable.img_hero_nebula
        "img_poster_chronicle" -> R.drawable.img_poster_chronicle
        "img_poster_odyssey" -> R.drawable.img_poster_odyssey
        "img_poster_horizon" -> R.drawable.img_poster_horizon
        else -> R.drawable.img_poster_chronicle
    }
}

@Composable
fun VeloraLogo(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 28.dp else 34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.linearGradient(
                        listOf(VeloraCrimson, VeloraCoral)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Notflix icon",
                tint = Color.White,
                modifier = Modifier.size(if (compact) 18.dp else 22.dp)
            )
        }
        if (!compact) {
            Text(
                text = "NOTFLIX",
                style = MaterialTheme.typography.titleLarge.copy(
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = VeloraOnBackground
            )
        }
    }
}

@Composable
fun CatalogImage(
    imageUrl: String?,
    fallbackKey: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val fallbackRes = resolveArtworkDrawable(fallbackKey)
    if (!imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            placeholder = painterResource(id = fallbackRes),
            error = painterResource(id = fallbackRes),
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(id = fallbackRes),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}

data class AvatarPalette(val key: String, val label: String, val colors: List<Color>)

val VeloraAvatarPalettes = listOf(
    AvatarPalette("crimson", "Crimson", listOf(Color(0xFFE50914), Color(0xFF7A0411))),
    AvatarPalette("sky", "Sky", listOf(Color(0xFF0EA5E9), Color(0xFF0369A1))),
    AvatarPalette("emerald", "Emerald", listOf(Color(0xFF10B981), Color(0xFF047857))),
    AvatarPalette("amber", "Amber", listOf(Color(0xFFF59E0B), Color(0xFFB45309))),
    AvatarPalette("violet", "Violet", listOf(Color(0xFF8B5CF6), Color(0xFF5B21B6))),
)

@Composable
fun ProfileAvatar(
    avatarKey: String,
    name: String,
    isKids: Boolean,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val palette = VeloraAvatarPalettes.firstOrNull { it.key == avatarKey } ?: VeloraAvatarPalettes.first()
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.22f))
            .background(Brush.linearGradient(palette.colors))
            .border(1.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(size * 0.22f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.trim().take(1).uppercase().ifBlank { "N" },
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = (size.value * 0.42f).sp,
                fontWeight = FontWeight.ExtraBold
            ),
            color = Color.White
        )
        if (isKids) {
            Surface(
                color = VeloraKidsCyan,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(3.dp)
            ) {
                Text(
                    text = "KIDS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
fun PosterCard(
    item: CatalogItem,
    onClick: () -> Unit,
    onQuickPreview: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 136.dp,
    progressFraction: Float? = null,
    subtitleOverride: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.04f else 1f,
        animationSpec = spring(),
        label = "poster_hover_scale"
    )

    Column(
        modifier = modifier
            .width(width)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(VeloraSurface)
            .border(1.dp, if (isHovered) VeloraCrimson else VeloraOutline, RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .testTag("poster_card_${item.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
        ) {
            CatalogImage(
                imageUrl = item.posterUrl,
                fallbackKey = item.posterKey,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.25f),
                            0.55f to Color.Transparent,
                            1f to VeloraBackground.copy(alpha = 0.92f)
                        )
                    )
            )
            // Top badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (item.isSeries) "SERIES" else "FILM",
                        style = MaterialTheme.typography.labelSmall,
                        color = VeloraCoral,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                IconButton(
                    onClick = onQuickPreview,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Quick preview ${item.title}",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            // Bottom title overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitleOverride ?: "${item.releaseYear} • ${item.ageRating}",
                    style = MaterialTheme.typography.labelSmall,
                    color = VeloraOnSurfaceMuted,
                    maxLines = 1
                )
            }
        }
        if (progressFraction != null) {
            LinearProgressIndicator(
                progress = { progressFraction.coerceIn(0.02f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = VeloraCrimson,
                trackColor = VeloraSurfaceBright
            )
        }
    }
}

@Composable
fun TopTenPosterCard(
    rank: Int,
    item: CatalogItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(172.dp)
            .height(206.dp)
            .clickable(onClick = onClick)
            .testTag("top_ten_card_${item.id}")
    ) {
        Text(
            text = rank.toString(),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 86.sp,
                fontWeight = FontWeight.ExtraBold
            ),
            color = VeloraSurfaceBright,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 2.dp, bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(126.dp)
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, VeloraOutline, RoundedCornerShape(12.dp))
        ) {
            CatalogImage(
                imageUrl = item.posterUrl,
                fallbackKey = item.posterKey,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.4f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.88f)
                        )
                    )
            )
            Surface(
                color = VeloraCrimson,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            ) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickPreviewBottomSheet(
    item: CatalogItem,
    inWatchlist: Boolean,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onToggleWatchlist: () -> Unit,
    onMoreInfo: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = VeloraSurfaceElevated,
        contentColor = VeloraOnBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                CatalogImage(
                    imageUrl = item.posterUrl,
                    fallbackKey = item.posterKey,
                    contentDescription = item.title,
                    modifier = Modifier
                        .width(96.dp)
                        .aspectRatio(0.72f)
                        .clip(RoundedCornerShape(10.dp))
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = VeloraOnBackground
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${(item.trendingScore / 10).coerceIn(82, 99)}% Match",
                            style = MaterialTheme.typography.labelLarge,
                            color = VeloraMatchGreen
                        )
                        Text(
                            text = "${item.releaseYear}",
                            style = MaterialTheme.typography.bodySmall,
                            color = VeloraOnSurfaceMuted
                        )
                        Surface(
                            color = VeloraSurfaceBright,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = item.ageRating,
                                style = MaterialTheme.typography.labelSmall,
                                color = VeloraOnBackground,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "${item.runtimeMinutes}m",
                            style = MaterialTheme.typography.bodySmall,
                            color = VeloraOnSurfaceMuted
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item.genres.forEach { genre ->
                            AssistChip(
                                onClick = {},
                                label = { Text(genre, style = MaterialTheme.typography.labelSmall) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = VeloraSurfaceBright,
                                    labelColor = VeloraOnBackground
                                ),
                                border = null
                            )
                        }
                    }
                }
            }

            Text(
                text = item.synopsis,
                style = MaterialTheme.typography.bodyMedium,
                color = VeloraOnSurfaceMuted,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPlay,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("preview_play_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Play")
                }
                FilledTonalButton(
                    onClick = onToggleWatchlist,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("preview_watchlist_button"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = VeloraSurfaceBright,
                        contentColor = VeloraOnBackground
                    )
                ) {
                    Icon(
                        imageVector = if (inWatchlist) Icons.Filled.Check else Icons.Filled.Add,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (inWatchlist) "Saved" else "My List")
                }
            }

            OutlinedButton(
                onClick = onMoreInfo,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("preview_more_info_button"),
                border = BorderStroke(1.dp, VeloraOutline)
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = VeloraGold)
                Spacer(Modifier.width(8.dp))
                Text("More Information & Episodes", color = VeloraOnBackground)
            }
        }
    }
}
