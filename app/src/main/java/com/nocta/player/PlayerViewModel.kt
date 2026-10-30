package com.nocta.player

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.nocta.player.data.Album
import com.nocta.player.data.DefaultAccent
import com.nocta.player.data.DefaultAccentSoft
import com.nocta.player.data.Lyrics
import com.nocta.player.data.LyricsState
import com.nocta.player.data.Song
import com.nocta.player.data.toMediaItem
import com.nocta.player.data.toSongOrNull
import com.nocta.player.playback.PlaybackService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NoctaApp
    val settings = app.settings
    private val musicRepo = app.music
    private val lyricsRepo = app.lyrics
    private val videoRepo = app.videos
    private val artworkStore = app.artwork

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _permissionGranted = MutableStateFlow(hasAudioPermission())
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    private val _current = MutableStateFlow<Song?>(null)
    val current: StateFlow<Song?> = _current.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _lyrics = MutableStateFlow<LyricsState>(LyricsState.Idle)
    val lyrics: StateFlow<LyricsState> = _lyrics.asStateFlow()

    private val _video = MutableStateFlow<Uri?>(null)
    val video: StateFlow<Uri?> = _video.asStateFlow()

    private val _accent = MutableStateFlow(DefaultAccent)
    val accent: StateFlow<Color> = _accent.asStateFlow()

    private val _accentSoft = MutableStateFlow(DefaultAccentSoft)
    val accentSoft: StateFlow<Color> = _accentSoft.asStateFlow()

    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search.asStateFlow()

    private val _showPlayer = MutableStateFlow(false)
    val showPlayer: StateFlow<Boolean> = _showPlayer.asStateFlow()

    private val _showLyrics = MutableStateFlow(false)
    val showLyrics: StateFlow<Boolean> = _showLyrics.asStateFlow()

    private val _showQueue = MutableStateFlow(false)
    val showQueue: StateFlow<Boolean> = _showQueue.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var controller: MediaController? = null
    private val controllerFuture = MediaController.Builder(
        app,
        SessionToken(app, ComponentName(app, PlaybackService::class.java))
    ).buildAsync()

    private var lyricsJob: Job? = null
    private var videoJob: Job? = null
    private var accentJob: Job? = null
    private var lastSongId: Long? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            _isPlaying.value = player.isPlaying
            _shuffle.value = player.shuffleModeEnabled
            _repeatMode.value = player.repeatMode

            if (player.duration > 0) _duration.value = player.duration
            if (player.currentPosition >= 0) _position.value = player.currentPosition

            val item = player.currentMediaItem
            val song = item?.toSongOrNull()
            if (song != null && song.id != lastSongId) {
                lastSongId = song.id
                _current.value = song
                settings.markPlayed(song.id)
                loadLyricsFor(song)
                loadVideoFor(song)
                updateAccent(song)
                _duration.value = song.durationMs
            } else if (song == null && lastSongId != null) {
                // Queue finished and was cleared.
                lastSongId = null
                _current.value = null
                _isPlaying.value = false
            }

            if (player.mediaItemCount != _queue.value.size) {
                syncQueue(player)
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val song = mediaItem?.toSongOrNull() ?: return
            _position.value = 0L
            if (song.id != lastSongId) {
                lastSongId = song.id
                _current.value = song
                settings.markPlayed(song.id)
                loadLyricsFor(song)
                loadVideoFor(song)
                updateAccent(song)
                _duration.value = song.durationMs
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _message.value = "Playback error: ${error.errorCodeName}"
        }
    }

    init {
        controllerFuture.addListener(
            {
                runCatching { controllerFuture.get() }.onSuccess { mediaController ->
                    controller = mediaController
                    mediaController.addListener(listener)
                    _isPlaying.value = mediaController.isPlaying
                    _shuffle.value = mediaController.shuffleModeEnabled
                    _repeatMode.value = mediaController.repeatMode

                    mediaController.currentMediaItem?.toSongOrNull()?.let { song ->
                        lastSongId = song.id
                        _current.value = song
                        _duration.value = song.durationMs
                        loadLyricsFor(song)
                        loadVideoFor(song)
                        updateAccent(song)
                    }
                    syncQueue(mediaController)
                    // The service may have been restarted with no media items; re-attach the
                    // library so the mini player and queue stay coherent.
                    _current.value?.let { ensureQueueAttached(it) }
                }
            },
            ContextCompat.getMainExecutor(app)
        )

        viewModelScope.launch {
            while (isActive) {
                controller?.let { player ->
                    if (player.currentPosition >= 0) _position.value = player.currentPosition
                    if (player.duration > 0) _duration.value = player.duration
                    _isPlaying.value = player.isPlaying
                }
                delay(220)
            }
        }

        viewModelScope.launch {
            settings.accentMode.collect {
                updateAccent(_current.value)
            }
        }

        if (_permissionGranted.value) refreshLibrary()
        else _isLoading.value = false
    }

    // ---------------------------------------------------------------- library

    fun refreshLibrary() {
        viewModelScope.launch {
            _isLoading.value = true
            val loaded = musicRepo.loadSongs()
            _songs.value = loaded
            _albums.value = musicRepo.toAlbums(loaded)
            _isLoading.value = false
            if (_current.value == null) {
                settings.recent.value.firstOrNull()?.let { recentId ->
                    loaded.firstOrNull { it.id == recentId }?.let { song ->
                        restoreLastSong(song, loaded)
                    }
                }
            } else {
                _current.value?.let { ensureQueueAttached(it) }
            }
        }
    }

    /**
     * Reopening the app after the playback service was killed leaves no active media item.
     * Show the last played track in the mini player, with the library loaded as the queue,
     * so pressing play continues where the user left off.
     */
    private fun restoreLastSong(song: Song, library: List<Song>) {
        lastSongId = song.id
        _current.value = song
        _duration.value = song.durationMs
        _position.value = 0L
        loadLyricsFor(song)
        loadVideoFor(song)
        updateAccent(song)
        ensureQueueAttached(song)
    }

    /** Loads the library into the (possibly empty) player so the queue and controls work. */
    private fun ensureQueueAttached(song: Song) {
        val player = controller ?: return
        if (player.mediaItemCount > 0) return
        val library = _songs.value
        if (library.isEmpty()) return
        val index = library.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        player.setMediaItems(library.map { it.toMediaItem() }, index, 0L)
        player.prepare()
        syncQueueFromList(library)
    }

    fun onPermissionsChanged() {
        val granted = hasAudioPermission()
        _permissionGranted.value = granted
        if (granted) refreshLibrary() else _isLoading.value = false
    }

    private fun hasAudioPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(app, permission) == PackageManager.PERMISSION_GRANTED
    }

    // ------------------------------------------------------------- transport

    fun playSong(song: Song, context: List<Song> = emptyList()) {
        val source = context.ifEmpty { _songs.value }
        if (source.isEmpty()) return
        val index = source.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        val items = source.map { it.toMediaItem() }
        controller?.apply {
            setMediaItems(items, index, 0L)
            prepare()
            play()
        }
        _current.value = song
        _duration.value = song.durationMs
        _position.value = 0L
        syncQueueFromList(source)
    }

    fun playAlbum(album: Album, startIndex: Int = 0) {
        val songs = album.songs
        if (songs.isEmpty()) return
        controller?.apply {
            setMediaItems(songs.map { it.toMediaItem() }, startIndex, 0L)
            prepare()
            play()
        }
        _current.value = songs[startIndex]
        _duration.value = songs[startIndex].durationMs
        _position.value = 0L
        syncQueueFromList(songs)
    }

    fun playPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        controller?.let {
            if (it.currentPosition > 3500) it.seekTo(0) else it.seekToPreviousMediaItem()
        }
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms.coerceAtLeast(0))
        _position.value = ms
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeat() {
        controller?.let {
            val next = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            it.repeatMode = next
        }
    }

    fun playQueueIndex(index: Int) {
        controller?.let {
            if (index in 0 until it.mediaItemCount) {
                it.seekTo(index, 0L)
                it.play()
            }
        }
    }

    fun toggleLikeCurrent(): Boolean {
        val song = _current.value ?: return false
        return settings.toggleLike(song.id)
    }

    fun toggleLike(id: Long): Boolean = settings.toggleLike(id)

    fun setSearch(value: String) {
        _search.value = value
    }

    fun openPlayer() {
        _showPlayer.value = true
    }

    fun closePlayer() {
        _showPlayer.value = false
        _showLyrics.value = false
    }

    fun openLyrics() {
        _showLyrics.value = true
    }

    fun closeLyrics() {
        _showLyrics.value = false
    }

    fun setQueueVisible(visible: Boolean) {
        _showQueue.value = visible
    }

    fun clearMessage() {
        _message.value = null
    }

    fun report(text: String) {
        _message.value = text
    }

    fun refreshSidecars() {
        app.sidecars.invalidate()
        _current.value?.let {
            loadVideoFor(it)
            loadLyricsFor(it, force = true)
        }
    }

    fun clearLyricsCache() {
        lyricsRepo.clearCache()
        _current.value?.let { loadLyricsFor(it, force = true) }
        _message.value = "Lyrics cache cleared"
    }

    fun retryLyrics() {
        _current.value?.let { loadLyricsFor(it, force = true) }
    }

    // ---------------------------------------------------------------- lyrics

    private fun loadLyricsFor(song: Song, force: Boolean = false) {
        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch {
            _lyrics.value = LyricsState.Loading
            _lyrics.value = lyricsRepo.lyricsFor(song, force)
        }
    }

    fun lyricsNow(): Lyrics? = (_lyrics.value as? LyricsState.Ready)?.lyrics

    private fun loadVideoFor(song: Song) {
        videoJob?.cancel()
        videoJob = viewModelScope.launch {
            _video.value = videoRepo.videoFor(song)
        }
    }

    private fun syncQueue(player: Player) {
        val list = ArrayList<Song>(player.mediaItemCount)
        for (i in 0 until player.mediaItemCount) {
            player.getMediaItemAt(i).toSongOrNull()?.let(list::add)
        }
        _queue.value = list
    }

    private fun syncQueueFromList(list: List<Song>) {
        _queue.value = list
    }

    private fun updateAccent(song: Song?) {
        when (settings.accentMode.value) {
            "violet" -> setAccent(Color(0xFFA78BFA), Color(0xFF5EEAD4))
            "mint" -> setAccent(Color(0xFF5EEAD4), Color(0xFFA7F3D0))
            "amber" -> setAccent(Color(0xFFFBBF24), Color(0xFFF97316))
            "rose" -> setAccent(Color(0xFFFB7185), Color(0xFFF9A8D4))
            "ice" -> setAccent(Color(0xFF93C5FD), Color(0xFF67E8F9))
            else -> {
                if (song == null) {
                    setAccent(DefaultAccent, DefaultAccentSoft)
                    return
                }
                accentJob?.cancel()
                accentJob = viewModelScope.launch {
                    val colors = artworkStore.colors(song)
                    if (colors != null) {
                        _accent.value = colors.primary
                        _accentSoft.value = colors.secondary
                    }
                }
            }
        }
    }

    private fun setAccent(primary: Color, soft: Color) {
        _accent.value = primary
        _accentSoft.value = soft
    }

    override fun onCleared() {
        MediaController.releaseFuture(controllerFuture)
        controller = null
        super.onCleared()
    }

    companion object {
        private const val DEFAULT_ACCENT = 0
    }
}
