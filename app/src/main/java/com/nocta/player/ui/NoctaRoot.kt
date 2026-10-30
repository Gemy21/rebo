package com.nocta.player.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nocta.player.PlayerViewModel
import com.nocta.player.ui.components.AlbumArt
import com.nocta.player.ui.components.BottomNav
import com.nocta.player.ui.components.EqualizerBars
import com.nocta.player.ui.components.FloatingToast
import com.nocta.player.ui.components.GlassChip
import com.nocta.player.ui.components.MainTab
import com.nocta.player.ui.components.MiniPlayer
import com.nocta.player.ui.components.NoctaBackground
import com.nocta.player.ui.screens.LibraryScreen
import com.nocta.player.ui.screens.LyricsScreen
import com.nocta.player.ui.screens.NowPlayingScreen
import com.nocta.player.ui.screens.SearchScreen
import com.nocta.player.ui.screens.SettingsScreen
import com.nocta.player.ui.theme.LocalAccent
import com.nocta.player.ui.theme.NoctaBlack
import com.nocta.player.ui.theme.NoctaMuted
import com.nocta.player.ui.theme.NoctaTheme
import kotlinx.coroutines.delay

@Composable
fun NoctaRoot(
    vm: PlayerViewModel,
    onRequestPermission: () -> Unit
) {
    val accent by vm.accent.collectAsState()
    val accentSoft by vm.accentSoft.collectAsState()
    val crossfade by vm.settings.crossfade.collectAsState()

    val accentSpec = if (crossfade) tween<androidx.compose.ui.graphics.Color>(700) else snap()
    val liveAccent by animateColorAsState(accent, accentSpec, label = "accent")
    val liveAccentSoft by animateColorAsState(accentSoft, accentSpec, label = "accentSoft")

    val song by vm.current.collectAsState()
    val isPlaying by vm.isPlaying.collectAsState()
    val positionState = vm.position.collectAsState()
    val duration by vm.duration.collectAsState()

    val showPlayer by vm.showPlayer.collectAsState()
    val showLyrics by vm.showLyrics.collectAsState()
    val showQueue by vm.showQueue.collectAsState()
    val message by vm.message.collectAsState()

    var tab by rememberSaveable { mutableStateOf(MainTab.Library) }

    NoctaTheme(accent = liveAccent, accentSoft = liveAccentSoft) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NoctaBlack)
        ) {
            NoctaBackground(
                modifier = Modifier.fillMaxSize()
            )

            Crossfade(
                targetState = tab,
                animationSpec = tween(260),
                label = "tab"
            ) { screen ->
                val listInset = if (song != null && !showPlayer) 196.dp else 136.dp
                when (screen) {
                    MainTab.Library -> LibraryScreen(
                        vm = vm,
                        onOpenSearch = { tab = MainTab.Search },
                        onRequestPermission = onRequestPermission,
                        bottomPadding = listInset
                    )

                    MainTab.Search -> SearchScreen(vm = vm, bottomPadding = listInset)
                    MainTab.Settings -> SettingsScreen(vm = vm, bottomPadding = listInset)
                }
            }

            // A light fade keeps rows from clashing with the navigation labels while still
            // letting the library scroll visibly through the glass bars.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(if (song != null && !showPlayer) 268.dp else 208.dp)
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.15f to Color.Black.copy(alpha = 0.30f),
                                0.30f to Color.Black.copy(alpha = 0.72f),
                                0.42f to Color.Black.copy(alpha = 0.88f),
                                0.55f to Color.Black.copy(alpha = 0.94f),
                                1f to Color.Black.copy(alpha = 0.97f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                AnimatedVisibility(
                    visible = song != null && !showPlayer,
                    enter = fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 3 },
                    exit = fadeOut(tween(160)) + slideOutVertically(tween(200)) { it / 3 }
                ) {
                    MiniPlayer(
                        song = song,
                        isPlaying = isPlaying,
                        position = positionState,
                        duration = duration,
                        onOpen = { vm.openPlayer() },
                        onToggle = { vm.playPause() },
                        onNext = { vm.next() }
                    )
                }

                Spacer(Modifier.height(8.dp))

                BottomNav(
                    selected = tab,
                    onSelect = { tab = it }
                )
            }

            AnimatedVisibility(
                visible = showPlayer,
                enter = slideInVertically(animationSpec = tween(340)) { it } + fadeIn(tween(180)),
                exit = slideOutVertically(animationSpec = tween(260)) { it } + fadeOut(tween(180))
            ) {
                NowPlayingScreen(
                    vm = vm,
                    onClose = { vm.closePlayer() },
                    onOpenLyrics = { vm.openLyrics() },
                    onOpenQueue = { vm.setQueueVisible(true) }
                )
            }

            AnimatedVisibility(
                visible = showLyrics,
                enter = slideInVertically(animationSpec = tween(320)) { it } + fadeIn(tween(200)),
                exit = slideOutVertically(animationSpec = tween(240)) { it } + fadeOut(tween(160))
            ) {
                LyricsScreen(
                    vm = vm,
                    onClose = { vm.closeLyrics() }
                )
            }

            AnimatedVisibility(
                visible = message != null,
                enter = fadeIn(tween(180)) + slideInVertically(tween(240)) { -it / 2 },
                exit = fadeOut(tween(160)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 14.dp, start = 24.dp, end = 24.dp)
            ) {
                FloatingToast(text = message.orEmpty())
            }

            if (showQueue) {
                QueueSheet(vm = vm, onDismiss = { vm.setQueueVisible(false) })
            }
        }

        LaunchedEffect(message) {
            if (message != null) {
                delay(2800)
                vm.clearMessage()
            }
        }

        BackHandler(enabled = showQueue) { vm.setQueueVisible(false) }
        BackHandler(enabled = !showQueue && showLyrics) { vm.closeLyrics() }
        BackHandler(enabled = !showQueue && !showLyrics && showPlayer) { vm.closePlayer() }
        BackHandler(
            enabled = !showQueue && !showLyrics && !showPlayer && tab != MainTab.Library
        ) { tab = MainTab.Library }
    }
}

@Composable
private fun QueueSheet(vm: PlayerViewModel, onDismiss: () -> Unit) {
    val queue by vm.queue.collectAsState()
    val current by vm.current.collectAsState()
    val isPlaying by vm.isPlaying.collectAsState()
    val accent = LocalAccent.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val currentIndex = queue.indexOfFirst { it.id == current?.id }

    LaunchedEffect(currentIndex, queue.size) {
        if (currentIndex > 0) {
            runCatching { listState.scrollToItem((currentIndex - 2).coerceAtLeast(0)) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xE60A0A0E),
        scrimColor = Color.Black.copy(alpha = 0.66f),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .size(width = 42.dp, height = 4.dp)
                    .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(50))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Up next",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Text(
                        text = "${queue.size} tracks in queue",
                        style = MaterialTheme.typography.bodySmall,
                        color = NoctaMuted
                    )
                }
                Spacer(Modifier.weight(1f))
                GlassChip(
                    text = if (isPlaying) "Playing" else "Paused",
                    selected = true,
                    onClick = { vm.playPause() }
                )
            }

            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nothing queued yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NoctaMuted
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.heightIn(max = 460.dp)
                ) {
                    itemsIndexed(queue, key = { _, song -> song.id }) { index, song ->
                        val active = current?.id == song.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (active) Color.White.copy(alpha = 0.05f) else Color.Transparent
                                )
                                .clickable { vm.playQueueIndex(index) }
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (active && isPlaying) {
                                    EqualizerBars(
                                        modifier = Modifier
                                            .width(14.dp)
                                            .height(12.dp),
                                        color = accent.primary,
                                        playing = true,
                                        barCount = 3
                                    )
                                } else {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = NoctaMuted
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            AlbumArt(
                                song = song,
                                modifier = Modifier.size(40.dp),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(Modifier.width(11.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = if (active) accent.primary else Color.White,
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
                            Icon(
                                imageVector = Icons.Rounded.DragIndicator,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.18f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
