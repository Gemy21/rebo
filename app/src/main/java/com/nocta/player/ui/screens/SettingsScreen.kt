package com.nocta.player.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nocta.player.PlayerViewModel
import com.nocta.player.ui.components.GlassChip
import com.nocta.player.ui.components.GlassSurface
import com.nocta.player.ui.components.LabelPill
import com.nocta.player.ui.theme.LocalAccent
import com.nocta.player.ui.theme.NoctaMuted

private data class AccentOption(val key: String, val label: String, val color: Color)

private val accentOptions = listOf(
    AccentOption("auto", "Album", Color(0xFFB7A6FF)),
    AccentOption("violet", "Violet", Color(0xFFA78BFA)),
    AccentOption("mint", "Mint", Color(0xFF5EEAD4)),
    AccentOption("amber", "Amber", Color(0xFFFBBF24)),
    AccentOption("rose", "Rose", Color(0xFFFB7185)),
    AccentOption("ice", "Ice", Color(0xFF93C5FD))
)

@Composable
fun SettingsScreen(
    vm: PlayerViewModel,
    bottomPadding: androidx.compose.ui.unit.Dp = 168.dp
) {
    val context = LocalContext.current
    val accent = LocalAccent.current
    val accentMode by vm.settings.accentMode.collectAsState()
    val canvasEnabled by vm.settings.canvasEnabled.collectAsState()
    val motionEnabled by vm.settings.motionEnabled.collectAsState()
    val karaokeFill by vm.settings.karaokeFill.collectAsState()
    val crossfade by vm.settings.crossfade.collectAsState()
    val songs by vm.songs.collectAsState()
    val albums by vm.albums.collectAsState()
    val treeUri = remember(vm.settings.treeUri) { vm.settings.treeUri }

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            vm.settings.treeUri = uri
            vm.refreshSidecars()
            vm.report("Folder linked — canvas clips and .lrc files unlocked")
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = bottomPadding),
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        item {
            Column {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White
                )
                Text(
                    text = "Tune the atmosphere",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NoctaMuted
                )
                Spacer(Modifier.height(20.dp))
            }
        }

        item {
            SectionCard(
                title = "Appearance",
                icon = Icons.Rounded.GraphicEq,
                trailing = {
                    LabelPill(
                        text = accentOptions.firstOrNull { it.key == accentMode }?.label ?: "Album",
                        tint = accent.primary
                    )
                }
            ) {
                Text(
                    text = "Accent",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "Auto follows the cover art of the current song.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NoctaMuted
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    accentOptions.forEach { option ->
                        AccentSwatch(
                            option = option,
                            selected = accentMode == option.key,
                            onClick = { vm.settings.setAccentMode(option.key) }
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

        item {
            SectionCard(
                title = "Liquid motion",
                icon = Icons.Rounded.Movie
            ) {
                ToggleRow(
                    title = "Canvas video",
                    subtitle = "Loop Artist - Title.mp4 next to imported songs",
                    checked = canvasEnabled,
                    onCheckedChange = { vm.settings.setCanvasEnabled(it) }
                )
                Divider()
                ToggleRow(
                    title = "Ambient motion",
                    subtitle = "Breathing artwork, drifting light, floating dust",
                    checked = motionEnabled,
                    onCheckedChange = { vm.settings.setMotionEnabled(it) }
                )
                Divider()
                ToggleRow(
                    title = "Karaoke lyrics",
                    subtitle = "Fill each line as the song plays",
                    checked = karaokeFill,
                    onCheckedChange = { vm.settings.setKaraokeFill(it) }
                )
                Divider()
                ToggleRow(
                    title = "Soft crossfade",
                    subtitle = "Blend accent colours as the next song starts",
                    checked = crossfade,
                    onCheckedChange = { vm.settings.setCrossfade(it) }
                )
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

        item {
            SectionCard(
                title = "Library",
                icon = Icons.Rounded.FolderOpen
            ) {
                InfoRow(
                    title = "Collection",
                    value = "${songs.size} tracks · ${albums.size} albums"
                )
                Divider()
                InfoRow(
                    title = "Imported",
                    value = "${songs.count { it.isImported }} tracks from Spotify Importer"
                )
                Divider()
                LinkRow(
                    title = if (treeUri == null) "Connect music folder" else "Music folder linked",
                    subtitle = if (treeUri == null) {
                        "Pick Music/SpotifyImporter to read .lrc lyrics and .mp4 canvases"
                    } else {
                        treeUri.lastPathSegment?.replace("primary:", "") ?: "Storage folder"
                    },
                    onClick = { folderLauncher.launch(null) }
                )
                Divider()
                LinkRow(
                    title = "Rescan library",
                    subtitle = "Pick up newly imported tracks",
                    onClick = {
                        vm.refreshLibrary()
                        vm.refreshSidecars()
                        vm.report("Library rescanned")
                    },
                    icon = Icons.Rounded.Refresh
                )
                Divider()
                LinkRow(
                    title = "Clear lyrics cache",
                    subtitle = "Refetch synced lyrics from LRCLIB",
                    onClick = { vm.clearLyricsCache() },
                    icon = Icons.Rounded.DeleteSweep
                )
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

        item {
            SectionCard(
                title = "About",
                icon = Icons.Rounded.Lyrics
            ) {
                InfoRow(title = "App", value = "Nocta 1.0.0")
                Divider()
                InfoRow(title = "Engine", value = "Media3 · ExoPlayer")
                Divider()
                InfoRow(title = "Lyrics", value = "Local .lrc + LRCLIB")
                Divider()
                InfoRow(title = "Source", value = "Spotify Importer downloads")
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "NOCTA",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 6.sp),
                    color = Color.White.copy(alpha = 0.35f)
                )
                Text(
                    text = "AMOLED · liquid glass · built for your imports",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun AccentSwatch(option: AccentOption, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(option.color, option.color.copy(alpha = 0.45f))
                    )
                )
                .then(
                    if (selected) {
                        Modifier.border(2.dp, Color.White, CircleShape)
                    } else {
                        Modifier.border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                    }
                )
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = option.label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
            color = if (selected) Color.White else NoctaMuted
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val accent = LocalAccent.current
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = accent.primary.copy(alpha = 0.85f)
            )
            Spacer(Modifier.weight(1f))
            trailing?.invoke()
        }
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val accent = LocalAccent.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = NoctaMuted
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = accent.primary,
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = Color.White.copy(alpha = 0.75f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.08f),
                uncheckedBorderColor = Color.White.copy(alpha = 0.16f)
            )
        )
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = NoctaMuted,
            modifier = Modifier.padding(start = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
private fun LinkRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    icon: ImageVector = Icons.Rounded.ChevronRight
) {
    val accent = LocalAccent.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = NoctaMuted,
                maxLines = 2
            )
        }
        Spacer(Modifier.width(12.dp))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent.primary.copy(alpha = 0.75f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.08f),
                        Color.Transparent
                    )
                )
            )
    )
}
