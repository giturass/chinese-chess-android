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
    private var backgroundPlayer: MediaPlayer? = null
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
        backgroundPlayer = MediaPlayer.create(appContext, R.raw.bgm)?.apply {
            isLooping = true
            setVolume(0.24f, 0.24f)
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
        backgroundPlayer?.release()
        backgroundPlayer = null
        soundPool.release()
    }

    companion object {
        const val PREFS_NAME = "game_audio"
        const val MUTED_KEY = "muted"
    }
}
