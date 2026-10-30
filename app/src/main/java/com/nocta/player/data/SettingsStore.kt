package com.nocta.player.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("nocta_prefs", Context.MODE_PRIVATE)

    private val _liked = MutableStateFlow(loadLiked())
    val liked: StateFlow<Set<Long>> = _liked.asStateFlow()

    private val _recent = MutableStateFlow(loadRecent())
    val recent: StateFlow<List<Long>> = _recent.asStateFlow()

    private val _accentMode = MutableStateFlow(prefs.getString(KEY_ACCENT, "auto") ?: "auto")
    val accentMode: StateFlow<String> = _accentMode.asStateFlow()

    private val _canvasEnabled = MutableStateFlow(prefs.getBoolean(KEY_CANVAS, true))
    val canvasEnabled: StateFlow<Boolean> = _canvasEnabled.asStateFlow()

    private val _motionEnabled = MutableStateFlow(prefs.getBoolean(KEY_MOTION, true))
    val motionEnabled: StateFlow<Boolean> = _motionEnabled.asStateFlow()

    private val _karaokeFill = MutableStateFlow(prefs.getBoolean(KEY_KARAOKE, true))
    val karaokeFill: StateFlow<Boolean> = _karaokeFill.asStateFlow()

    private val _crossfade = MutableStateFlow(prefs.getBoolean(KEY_CROSSFADE, true))
    val crossfade: StateFlow<Boolean> = _crossfade.asStateFlow()

    var treeUri: Uri?
        get() = prefs.getString(KEY_TREE, null)?.let(Uri::parse)
        set(value) {
            prefs.edit().putString(KEY_TREE, value?.toString()).apply()
        }

    var hasSeenIntro: Boolean
        get() = prefs.getBoolean(KEY_INTRO, false)
        set(value) {
            prefs.edit().putBoolean(KEY_INTRO, value).apply()
        }

    val likedIds: Set<Long> get() = _liked.value

    fun toggleLike(id: Long): Boolean {
        val nowLiked = id !in _liked.value
        val next = if (nowLiked) _liked.value + id else _liked.value - id
        _liked.value = next
        prefs.edit().putStringSet(KEY_LIKED, next.map(Long::toString).toSet()).apply()
        return nowLiked
    }

    fun markPlayed(id: Long) {
        val next = (listOf(id) + _recent.value.filterNot { it == id }).take(30)
        _recent.value = next
        prefs.edit().putString(KEY_RECENT, next.joinToString(",")).apply()
    }

    fun setAccentMode(mode: String) {
        _accentMode.value = mode
        prefs.edit().putString(KEY_ACCENT, mode).apply()
    }

    fun setCanvasEnabled(enabled: Boolean) {
        _canvasEnabled.value = enabled
        prefs.edit().putBoolean(KEY_CANVAS, enabled).apply()
    }

    fun setMotionEnabled(enabled: Boolean) {
        _motionEnabled.value = enabled
        prefs.edit().putBoolean(KEY_MOTION, enabled).apply()
    }

    fun setKaraokeFill(enabled: Boolean) {
        _karaokeFill.value = enabled
        prefs.edit().putBoolean(KEY_KARAOKE, enabled).apply()
    }

    fun setCrossfade(enabled: Boolean) {
        _crossfade.value = enabled
        prefs.edit().putBoolean(KEY_CROSSFADE, enabled).apply()
    }

    private fun loadLiked(): Set<Long> =
        prefs.getStringSet(KEY_LIKED, emptySet())
            .orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .toSet()

    private fun loadRecent(): List<Long> =
        prefs.getString(KEY_RECENT, null)
            ?.split(",")
            ?.mapNotNull { it.toLongOrNull() }
            .orEmpty()

    private companion object {
        const val KEY_LIKED = "liked"
        const val KEY_RECENT = "recent"
        const val KEY_ACCENT = "accent_mode"
        const val KEY_CANVAS = "canvas_enabled"
        const val KEY_MOTION = "motion_enabled"
        const val KEY_KARAOKE = "karaoke_fill"
        const val KEY_CROSSFADE = "crossfade"
        const val KEY_TREE = "tree_uri"
        const val KEY_INTRO = "seen_intro"
    }
}
