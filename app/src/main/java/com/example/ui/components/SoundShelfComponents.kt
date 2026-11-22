package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.core.designsystem.AnimationIntensity
import com.example.core.designsystem.ArtworkPresentation
import com.example.core.designsystem.CardStyle
import com.example.core.designsystem.LocalSoundShelfTokens
import com.example.core.model.MusicTrack
import com.example.core.model.ProviderId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun ThemedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val tokens = LocalSoundShelfTokens.current
    val shape = RoundedCornerShape(tokens.cardCornerRadius)
    val containerColor = when (tokens.cardStyle) {
        CardStyle.ELEVATED -> tokens.palette.surfaceElevated
        CardStyle.GLASS -> tokens.palette.surfaceGlass
        CardStyle.OUTLINED -> tokens.palette.surface
        CardStyle.FLAT -> tokens.palette.surface.copy(alpha = 0.72f)
    }
    val border = when (tokens.cardStyle) {
        CardStyle.OUTLINED -> BorderStroke(1.dp, tokens.palette.outline)
        CardStyle.GLASS -> BorderStroke(1.dp, tokens.palette.outline.copy(alpha = 0.6f))
        else -> null
    }
    val elevation = if (tokens.cardStyle == CardStyle.ELEVATED) 4.dp else 0.dp

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = border,
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            content = content
        )
    }
}

@Composable
fun TrackArtwork(
    artworkUri: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    isSpinning: Boolean = false,
    forceSquare: Boolean = false
) {
    val tokens = LocalSoundShelfTokens.current
    val context = LocalContext.current
    val shape: Shape = if (forceSquare) {
        RoundedCornerShape(12.dp)
    } else when (tokens.artworkPresentation) {
        ArtworkPresentation.VINYL_DISC -> CircleShape
        ArtworkPresentation.ROUNDED -> RoundedCornerShape(tokens.artworkCornerRadius)
        ArtworkPresentation.SHARP_FRAME -> RoundedCornerShape(4.dp)
    }

    val shouldSpin = isSpinning &&
        tokens.artworkPresentation == ArtworkPresentation.VINYL_DISC &&
        tokens.animationIntensity != AnimationIntensity.MINIMAL

    val rotation = if (shouldSpin) {
        val infinite = rememberInfiniteTransition(label = "vinylSpin")
        val angle by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "vinylAngle"
        )
        angle
    } else 0f

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(tokens.palette.surfaceElevated)
            .rotate(rotation),
        contentAlignment = Alignment.Center
    ) {
        if (!artworkUri.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(artworkUri)
                    .crossfade(tokens.animationIntensity != AnimationIntensity.MINIMAL)
                    .error(R.drawable.img_vinyl_placeholder)
                    .fallback(R.drawable.img_vinyl_placeholder)
                    .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.img_vinyl_placeholder),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (tokens.artworkPresentation == ArtworkPresentation.VINYL_DISC && !forceSquare && size >= 64.dp) {
            Box(
                modifier = Modifier
                    .size(size * 0.2f)
                    .clip(CircleShape)
                    .background(tokens.palette.background.copy(alpha = 0.9f))
            )
        }
    }
}

@Composable
fun ProviderBadge(providerId: ProviderId, modifier: Modifier = Modifier) {
    val color = when (providerId) {
        ProviderId.LOCAL -> MaterialTheme.colorScheme.primary
        ProviderId.AUDIUS -> Color(0xFFCC0FE0)
        ProviderId.JAMENDO -> Color(0xFF10B981)
        ProviderId.YOUTUBE_MUSIC_EXPERIMENTAL -> Color(0xFFF59E0B)
        ProviderId.SPOTIFY -> Color(0xFF1DB954)
        ProviderId.EXTERNAL_STORE -> MaterialTheme.colorScheme.secondary
    }
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = providerId.badgeLabel,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun TrackRow(
    track: MusicTrack,
    isCurrent: Boolean,
    hapticsEnabled: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShowAudioInfo: () -> Unit,
    onEditMetadata: (() -> Unit)? = null,
    onFindDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrackArtwork(
            artworkUri = track.artworkUri,
            contentDescription = "${track.title} artwork",
            size = 50.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (track.providerId != ProviderId.LOCAL) {
                    ProviderBadge(track.providerId)
                }
                Text(
                    text = "${track.artist} · ${formatDuration(track.durationMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(
            onClick = {
                if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggleFavorite()
            },
            modifier = Modifier.minimumInteractiveComponentSize()
        ) {
            Icon(
                imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Toggle favourite",
                tint = if (track.isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Track options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                if (track.canJoinUnifiedQueue) {
                    DropdownMenuItem(text = { Text("Play Next") }, onClick = { menuExpanded = false; onPlayNext() })
                    DropdownMenuItem(text = { Text("Add to Queue") }, onClick = { menuExpanded = false; onAddToQueue() })
                    DropdownMenuItem(text = { Text("Add to Playlist") }, onClick = { menuExpanded = false; onAddToPlaylist() })
                }
                DropdownMenuItem(text = { Text("Audio Quality Info") }, onClick = { menuExpanded = false; onShowAudioInfo() })
                if (onEditMetadata != null && track.providerId == ProviderId.LOCAL) {
                    DropdownMenuItem(text = { Text("Edit Tags & Artwork") }, onClick = { menuExpanded = false; onEditMetadata() })
                }
                DropdownMenuItem(text = { Text("Find Legitimate / Better Copy") }, onClick = { menuExpanded = false; onFindDownload() })
            }
        }
    }
}

@Composable
fun MiniPlayerBar(
    track: MusicTrack,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onExpand: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalSoundShelfTokens.current
    val progress = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("mini_player_bar")
            .clickable(onClick = onExpand),
        shape = RoundedCornerShape(tokens.cardCornerRadius),
        color = tokens.palette.surfaceElevated,
        tonalElevation = 6.dp,
        border = BorderStroke(1.dp, tokens.palette.outline.copy(alpha = 0.5f))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrackArtwork(
                    artworkUri = track.artworkUri,
                    contentDescription = "${track.title} cover",
                    size = 44.dp,
                    isSpinning = isPlaying
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onOpenQueue,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("mini_queue_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Open queue")
                }
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("mini_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("mini_next_button")
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next track")
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun rememberExtractedArtworkBrush(artworkUri: String?, fallbackPrimary: Color, fallbackEnd: Color): Brush {
    val context = LocalContext.current
    var extractedColor by remember(artworkUri) { mutableStateOf<Color?>(null) }

    LaunchedEffect(artworkUri) {
        if (artworkUri.isNullOrBlank()) {
            extractedColor = null
            return@LaunchedEffect
        }
        extractedColor = extractDominantColor(context, artworkUri)
    }

    val topColor = (extractedColor ?: fallbackPrimary).copy(alpha = 0.45f)
    return Brush.verticalGradient(colors = listOf(topColor, fallbackEnd))
}

private suspend fun extractDominantColor(context: Context, uriString: String): Color? =
    withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(uriString)
            val bitmap: Bitmap? = if (uri.scheme == "content" || uri.scheme == "file") {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    BitmapFactory.decodeStream(stream, null, opts)
                }
            } else null

            if (bitmap != null) {
                val palette = Palette.from(bitmap).generate()
                val rgb = palette.getVibrantColor(palette.getDominantColor(0))
                bitmap.recycle()
                if (rgb != 0) Color(rgb) else null
            } else null
        }.getOrNull()
    }

fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}
