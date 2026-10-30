package com.nocta.player.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.nocta.player.PlayerViewModel
import com.nocta.player.ui.components.AlbumArt
import com.nocta.player.ui.components.GlassChip
import com.nocta.player.ui.components.GlassSurface
import com.nocta.player.ui.components.LabelPill
import com.nocta.player.ui.components.SongRow
import com.nocta.player.ui.theme.LocalAccent
import com.nocta.player.ui.theme.NoctaMuted

@Composable
fun SearchScreen(
    vm: PlayerViewModel,
    bottomPadding: androidx.compose.ui.unit.Dp = 168.dp
) {
    val query by vm.search.collectAsState()
    val songs by vm.songs.collectAsState()
    val likedIds by vm.settings.liked.collectAsState()
    val recentIds by vm.settings.recent.collectAsState()
    val current by vm.current.collectAsState()
    val isPlaying by vm.isPlaying.collectAsState()

    val accent = LocalAccent.current
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    var importedOnly by rememberSaveable { mutableStateOf(false) }
    var likedOnly by rememberSaveable { mutableStateOf(false) }

    val results = remember(query, songs, likedIds, importedOnly, likedOnly) {
        val q = query.trim()
        songs.filter { song ->
            val matchesQuery = q.isEmpty() ||
                song.title.contains(q, ignoreCase = true) ||
                song.artist.contains(q, ignoreCase = true) ||
                song.album.contains(q, ignoreCase = true)
            matchesQuery &&
                (!importedOnly || song.isImported) &&
                (!likedOnly || song.id in likedIds)
        }
    }

    val recentSongs = remember(recentIds, songs) {
        recentIds.mapNotNull { id -> songs.firstOrNull { it.id == id } }.take(8)
    }

    val browsing = query.isBlank() && !importedOnly && !likedOnly

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    LaunchedEffect(importedOnly, likedOnly) {
        if (query.isNotBlank()) keyboard?.hide()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Search",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White
            )
            Text(
                text = "${songs.size} tracks on this device",
                style = MaterialTheme.typography.bodyMedium,
                color = NoctaMuted
            )

            Spacer(Modifier.height(16.dp))

            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                intensity = 1.1f
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(11.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        BasicTextField(
                            value = query,
                            onValueChange = { vm.setSearch(it) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(accent.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = { keyboard?.hide() }
                            ),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (query.isEmpty()) {
                                        Text(
                                            text = "Songs, artists, albums…",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = NoctaMuted
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    }
                    if (query.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(50))
                                .clickable { vm.setSearch("") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    GlassChip(
                        text = "Imported",
                        selected = importedOnly,
                        onClick = {
                            importedOnly = !importedOnly
                            if (importedOnly) likedOnly = false
                            keyboard?.hide()
                        }
                    )
                }
                item {
                    GlassChip(
                        text = "Liked",
                        selected = likedOnly,
                        onClick = {
                            likedOnly = !likedOnly
                            if (likedOnly) importedOnly = false
                            keyboard?.hide()
                        }
                    )
                }
                item {
                    GlassChip(
                        text = "Clear",
                        selected = false,
                        onClick = {
                            vm.setSearch("")
                            importedOnly = false
                            likedOnly = false
                            keyboard?.hide()
                        }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
        }

        if (browsing) {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 6.dp,
                    bottom = bottomPadding
                )
            ) {
                if (recentSongs.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recently played",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }
                    items(recentSongs, key = { it.id }) { song ->
                        SongRow(
                            song = song,
                            isPlaying = current?.id == song.id && isPlaying,
                            onClick = { vm.playSong(song, recentSongs) }
                        )
                    }
                }
                item {
                    LikedTeaser(
                        likedCount = songs.count { it.id in likedIds },
                        modifier = Modifier.padding(top = 22.dp),
                        onClick = { likedOnly = true }
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 6.dp,
                    bottom = bottomPadding
                )
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when {
                                query.isNotBlank() -> "Results"
                                importedOnly -> "From Spotify Importer"
                                else -> "Liked songs"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        LabelPill(text = results.size.toString(), tint = accent.primary)
                        Spacer(Modifier.weight(1f))
                        if (results.isNotEmpty()) {
                            GlassChip(
                                text = "Play all",
                                selected = true,
                                onClick = { vm.playSong(results.first(), results) }
                            )
                        }
                    }
                }

                items(results, key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        isPlaying = current?.id == song.id && isPlaying,
                        onClick = { vm.playSong(song, results) },
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }

                if (results.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 70.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = when {
                                    query.isNotBlank() -> "No matches for “$query”"
                                    importedOnly -> "Nothing imported yet"
                                    likedOnly -> "No liked songs yet"
                                    else -> "Nothing here yet"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = when {
                                    query.isNotBlank() -> "Try a different spelling or search by album."
                                    importedOnly -> "Download songs with Spotify Importer to fill this shelf."
                                    likedOnly -> "Tap the heart on any song to save it here."
                                    else -> "Search for a song, artist or album."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = NoctaMuted,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LikedTeaser(
    likedCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accent = LocalAccent.current
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArt(
                song = null,
                modifier = Modifier.size(54.dp),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = "Liked songs",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = if (likedCount == 0) {
                        "Tap a heart to start the collection"
                    } else {
                        "$likedCount saved with a tap"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = NoctaMuted
                )
            }
            Spacer(Modifier.weight(1f))
            LabelPill(text = "Collection", tint = accent.soft)
        }
    }
}
