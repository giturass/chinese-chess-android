package com.ericlee.chess.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.ericlee.chess.R

class GameAudio(context: Context, initialMuted: Boolean = false) {
    private val appContext = context.applicationContext
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val clickSoundId = soundPool.load(appContext, R.raw.click, 1)
    private val backgroundTracks = intArrayOf(R.raw.bgm, R.raw.bg)
    private var backgroundPlayer: MediaPlayer? = null
    private var backgroundTrackIndex = 0
    private var muted = initialMuted
    private var released = false

    fun setMuted(value: Boolean) {
        if (released || muted == value) return
        muted = value
        if (muted) {
            pauseBackground()
        } else {
            resumeBackground()
        }
    }

    fun startBackground() {
        if (released || muted || backgroundPlayer != null) return
        val player = MediaPlayer.create(appContext, backgroundTracks[backgroundTrackIndex]) ?: return
        backgroundPlayer = player.apply {
            isLooping = false
            setVolume(0.24f, 0.24f)
            setOnCompletionListener { completedPlayer ->
                completedPlayer.setOnCompletionListener(null)
                completedPlayer.release()
                if (backgroundPlayer === completedPlayer) {
                    backgroundPlayer = null
                }
                backgroundTrackIndex = (backgroundTrackIndex + 1) % backgroundTracks.size
                if (!released && !muted) {
                    startBackground()
                }
            }
            start()
        }
    }

    fun resumeBackground() {
        if (released || muted) return
        val player = backgroundPlayer
        if (player == null) {
            startBackground()
        } else if (!player.isPlaying) {
            player.start()
        }
    }

    fun pauseBackground() {
        backgroundPlayer?.takeIf { it.isPlaying }?.pause()
    }

    fun playMove(capture: Boolean) {
        if (released || muted) return
        val volume = if (capture) 0.88f else 0.72f
        soundPool.play(clickSoundId, volume, volume, 1, 0, 1f)
    }

    fun release() {
        released = true
        backgroundPlayer?.setOnCompletionListener(null)
        backgroundPlayer?.release()
        backgroundPlayer = null
        soundPool.release()
    }

    companion object {
        const val PREFS_NAME = "game_audio"
        const val MUTED_KEY = "muted"
    }
}
