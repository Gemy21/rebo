package com.nocta.player

import android.app.Application
import coil.Coil
import coil.ImageLoader
import com.nocta.player.data.ArtFetcherFactory
import com.nocta.player.data.ArtKeyer
import com.nocta.player.data.ArtworkStore
import com.nocta.player.data.LyricsRepository
import com.nocta.player.data.MusicRepository
import com.nocta.player.data.SettingsStore
import com.nocta.player.data.SidecarIndex
import com.nocta.player.data.VideoRepository

class NoctaApp : Application() {

    lateinit var settings: SettingsStore
        private set
    lateinit var sidecars: SidecarIndex
        private set
    lateinit var artwork: ArtworkStore
        private set
    lateinit var music: MusicRepository
        private set
    lateinit var lyrics: LyricsRepository
        private set
    lateinit var videos: VideoRepository
        private set

    override fun onCreate() {
        super.onCreate()

        settings = SettingsStore(this)
        sidecars = SidecarIndex(this, settings)
        artwork = ArtworkStore(this)
        music = MusicRepository(this)
        lyrics = LyricsRepository(this, sidecars)
        videos = VideoRepository(sidecars)

        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .components {
                    add(ArtKeyer())
                    add(ArtFetcherFactory(artwork, this@NoctaApp))
                }
                .crossfade(280)
                .build()
        )
    }
}
