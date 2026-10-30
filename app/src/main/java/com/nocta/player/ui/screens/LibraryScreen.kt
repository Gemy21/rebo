package com.nocta.player.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nocta.player.PlayerViewModel
import com.nocta.player.data.Album
import com.nocta.player.data.Song
import com.nocta.player.ui.components.AlbumArt
import com.nocta.player.ui.components.EmptyState
import com.nocta.player.ui.components.GlassChip
import com.nocta.player.ui.components.GlassSurface
import com.nocta.player.ui.components.LabelPill
import com.nocta.player.ui.components.SongRow
import com.nocta.player.ui.theme.LocalAccent
import com.nocta.player.ui.theme.NoctaMuted
import java.util.Calendar

private enum class LibraryFilter(val label: String) {
    All("All"),
    Imported("Imported"),
    Albums("Albums"),
    Liked("Liked")
}

@Composable
fun LibraryScreen(
    vm: PlayerViewModel,
    onOpenSearch: () -> Unit,
    onRequestPermission: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 168.dp
) {
    val songs by vm.songs.collectAsState()
    val albums by vm.albums.collectAsState()
    val likedIds by vm.settings.liked.collectAsState()
    val recents by vm.settings.recent.collectAsState()
    val current by vm.current.collectAsState()
    val isPlaying by vm.isPlaying.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val permissionGranted by vm.permissionGranted.collectAsState()

    var filter by rememberSaveable { mutableStateOf(LibraryFilter.All) }

    val filteredSongs = remember(songs, filter, likedIds) {
        when (filter) {
            LibraryFilter.Imported -> songs.filter { it.isImported }
            LibraryFilter.Liked -> songs.filter { it.id in likedIds }
            else -> songs
        }
    }

    val recentSongs = remember(recents, songs) {
        recents.mapNotNull { id -> songs.firstOrNull { it.id == id } }.take(10)
    }

    val contentPadding = PaddingValues(
        start = 16.dp,
        end = 16.dp,
        top = 6.dp,
        bottom = bottomPadding
    )

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            !permissionGranted -> EmptyState(
                title = "Your music, untapped",
                subtitle = "Nocta needs access to your audio library to play the tracks you imported.",
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 120.dp),
                action = {
                    GlassChip(text = "Allow access", selected = true, onClick = onRequestPermission)
                }
            )

            isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    com.nocta.player.ui.components.EqualizerBars(
                        modifier = Modifier
                            .width(38.dp)
                            .height(26.dp),
                        color = LocalAccent.current.primary,
                        playing = true,
                        barCount = 5
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Reading your library",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NoctaMuted
                    )
                }
            }

            songs.isEmpty() -> EmptyState(
                title = "Nothing here yet",
                subtitle = "Import songs with the Spotify Importer app, then pull to refresh. Files appear in Music/SpotifyImporter.",
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 120.dp),
                action = {
                    GlassChip(
                        text = "Refresh library",
                        selected = true,
                        onClick = { vm.refreshLibrary() }
                    )
                }
            )

            filter == LibraryFilter.Albums -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = contentPadding,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LibraryHeader(
                        songCount = songs.size,
                        onOpenSearch = onOpenSearch,
                        filter = filter,
                        onFilterChange = { filter = it }
                    )
                }
                items(albums, key = { it.key }) { album ->
                    AlbumCard(
                        album = album,
                        onClick = { vm.playAlbum(album) }
                    )
                }
            }

            else -> LazyColumn(
                state = rememberLazyListState(),
                contentPadding = contentPadding,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                item(key = "header") {
                    LibraryHeader(
                        songCount = songs.size,
                        onOpenSearch = onOpenSearch,
                        filter = filter,
                        onFilterChange = { filter = it }
                    )
                }

                if (filter == LibraryFilter.All && recentSongs.isNotEmpty()) {
                    item(key = "recent") {
                        RecentRow(
                            songs = recentSongs,
                            currentId = current?.id,
                            isPlaying = isPlaying,
                            onClick = { song -> vm.playSong(song, recentSongs) }
                        )
                    }
                }

                item(key = "section") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp, bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (filter) {
                                    LibraryFilter.Imported -> "From Spotify Importer"
                                    LibraryFilter.Liked -> "Liked songs"
                                    else -> "All songs"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                            LabelPill(
                                text = filteredSongs.size.toString(),
                                tint = LocalAccent.current.primary
                            )
                        }
                        if (filter == LibraryFilter.Imported) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Scanned from Music/SpotifyImporter",
                                style = MaterialTheme.typography.labelSmall,
                                color = NoctaMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                items(filteredSongs, key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        isPlaying = current?.id == song.id && isPlaying,
                        onClick = { vm.playSong(song, filteredSongs) },
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }

                if (filteredSongs.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (filter == LibraryFilter.Liked) {
                                    "Songs you like will show up here"
                                } else {
                                    "Nothing in this view yet"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = NoctaMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryHeader(
    songCount: Int,
    onOpenSearch: () -> Unit,
    filter: LibraryFilter,
    onFilterChange: (LibraryFilter) -> Unit
) {
    val accent = LocalAccent.current

    Column {
        Text(
            text = greeting(),
            style = MaterialTheme.typography.labelLarge,
            color = NoctaMuted
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Your library",
            style = MaterialTheme.typography.displayMedium,
            color = Color.White
        )

        Spacer(Modifier.height(16.dp))

        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(18.dp),
            onClick = onOpenSearch
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
                Text(
                    text = "Search songs, albums, artists",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NoctaMuted,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                LabelPill(text = "$songCount tracks", tint = accent.primary)
            }
        }

        Spacer(Modifier.height(14.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(LibraryFilter.entries.toList(), key = { it.name }) { item ->
                GlassChip(
                    text = item.label,
                    selected = filter == item,
                    onClick = { onFilterChange(item) }
                )
            }
        }
    }
}

@Composable
private fun RecentRow(
    songs: List<Song>,
    currentId: Long?,
    isPlaying: Boolean,
    onClick: (Song) -> Unit
) {
    Column(modifier = Modifier.padding(top = 22.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recently played",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Spacer(Modifier.weight(1f))
            LabelPill(text = "Jump back in", tint = NoctaMuted)
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(
            state = remember(songs) { androidx.compose.foundation.lazy.LazyListState() },
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                RecentCard(
                    song = song,
                    active = song.id == currentId,
                    isPlaying = isPlaying,
                    onClick = { onClick(song) }
                )
            }
        }
    }
}

@Composable
private fun RecentCard(song: Song, active: Boolean, isPlaying: Boolean, onClick: () -> Unit) {
    val accent = LocalAccent.current
    Column(
        modifier = Modifier
            .width(132.dp)
            .clickable(onClick = onClick)
    ) {
        Box {
            AlbumArt(
                song = song,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(20.dp)
            )
            if (active) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .background(accent.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    com.nocta.player.ui.components.EqualizerBars(
                        modifier = Modifier
                            .width(12.dp)
                            .height(10.dp),
                        color = Color.Black,
                        playing = isPlaying,
                        barCount = 3
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.displayArtist,
            style = MaterialTheme.typography.labelMedium,
            color = NoctaMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AlbumCard(album: Album, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        AlbumArt(
            song = album.first,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(22.dp)
        )
        Spacer(Modifier.height(9.dp))
        Text(
            text = album.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${album.artist} · ${album.songs.size} songs",
            style = MaterialTheme.typography.labelMedium,
            color = NoctaMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Late night"
    }
}
