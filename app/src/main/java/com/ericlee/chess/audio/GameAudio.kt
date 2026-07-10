package com.ericlee.chess.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class GameAudio {
    private var bgmTrack: AudioTrack? = null
    private val bgmSamples by lazy { createBackgroundLoop() }
    @Volatile
    private var released = false

    fun startBackground() {
        if (released || bgmTrack != null) return
        val track = buildTrack(
            sampleCount = bgmSamples.size,
            usage = AudioAttributes.USAGE_GAME,
            contentType = AudioAttributes.CONTENT_TYPE_MUSIC,
            mode = AudioTrack.MODE_STATIC
        )
        track.write(bgmSamples, 0, bgmSamples.size)
        track.setLoopPoints(0, bgmSamples.size, -1)
        track.setVolume(0.075f)
        track.play()
        bgmTrack = track
    }

    fun resumeBackground() {
        if (released) return
        val track = bgmTrack
        if (track == null) {
            startBackground()
        } else {
            track.play()
        }
    }

    fun pauseBackground() {
        bgmTrack?.pause()
    }

    fun playMove(capture: Boolean) {
        if (released) return
        val samples = if (capture) createCaptureSound() else createMoveSound()
        playOneShot(samples, if (capture) 0.48f else 0.34f)
    }

    fun release() {
        released = true
        bgmTrack?.run {
            runCatching { stop() }
            release()
        }
        bgmTrack = null
    }

    private fun playOneShot(samples: ShortArray, volume: Float) {
        thread(name = "chess-audio-effect", isDaemon = true) {
            val track = buildTrack(
                sampleCount = samples.size,
                usage = AudioAttributes.USAGE_GAME,
                contentType = AudioAttributes.CONTENT_TYPE_SONIFICATION,
                mode = AudioTrack.MODE_STATIC
            )
            track.write(samples, 0, samples.size)
            track.setVolume(volume)
            track.play()
            Thread.sleep(samples.size * 1000L / SAMPLE_RATE + 80L)
            track.release()
        }
    }

    private fun buildTrack(
        sampleCount: Int,
        usage: Int,
        contentType: Int,
        mode: Int
    ): AudioTrack = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(usage)
                .setContentType(contentType)
                .build()
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
        )
        .setTransferMode(mode)
        .setBufferSizeInBytes(sampleCount * BYTES_PER_SAMPLE)
        .build()

    private fun createMoveSound(): ShortArray {
        val count = (SAMPLE_RATE * 0.12f).toInt()
        return ShortArray(count) { index ->
            val t = index / SAMPLE_RATE.toFloat()
            val envelope = exp(-34f * t)
            val knock = sin(TWO_PI * 430f * t) * envelope
            val wood = sin(TWO_PI * 1320f * t) * exp(-58f * t) * 0.38f
            ((knock + wood) * Short.MAX_VALUE * 0.72f).toInt().toShort()
        }
    }

    private fun createCaptureSound(): ShortArray {
        val count = (SAMPLE_RATE * 0.24f).toInt()
        return ShortArray(count) { index ->
            val t = index / SAMPLE_RATE.toFloat()
            val thump = sin(TWO_PI * 168f * t) * exp(-18f * t)
            val ring = (
                sin(TWO_PI * 690f * t) +
                    sin(TWO_PI * 1035f * t) * 0.55f
                ) * exp(-11f * t)
            val edge = sin(TWO_PI * 1980f * t) * exp(-42f * t) * 0.32f
            ((thump * 0.78f + ring * 0.34f + edge) * Short.MAX_VALUE * 0.58f).toInt().toShort()
        }
    }

    private fun createBackgroundLoop(): ShortArray {
        val beats = intArrayOf(0, 2, 4, 5, 4, 2, 1, 2, 4, 7, 5, 4, 2, 0, -3, 0)
        val noteLength = 0.72f
        val count = (SAMPLE_RATE * noteLength * beats.size).toInt()
        val root = 293.66f
        return ShortArray(count) { index ->
            val t = index / SAMPLE_RATE.toFloat()
            val beat = ((t / noteLength).toInt()).coerceIn(0, beats.lastIndex)
            val localT = t - beat * noteLength
            val freq = root * pitchRatio(beats[beat])
            val pluck = exp(-3.8f * localT) * sin(TWO_PI * freq * localT)
            val overtone = exp(-5.2f * localT) * sin(TWO_PI * freq * 2.01f * localT) * 0.34f
            val breath = sin(TWO_PI * 73.42f * t) * 0.13f + sin(TWO_PI * 146.84f * t) * 0.05f
            val fade = loopFade(index, count)
            ((pluck * 0.48f + overtone + breath) * fade * Short.MAX_VALUE * 0.34f).toInt().toShort()
        }
    }

    private fun pitchRatio(steps: Int): Float {
        val scale = intArrayOf(0, 2, 4, 7, 9)
        val octave = Math.floorDiv(steps, scale.size)
        val degree = Math.floorMod(steps, scale.size)
        val semitones = scale[degree] + octave * 12
        return Math.pow(2.0, semitones / 12.0).toFloat()
    }

    private fun loopFade(index: Int, count: Int): Float {
        val fadeSamples = (SAMPLE_RATE * 0.2f).toInt()
        val head = (index / fadeSamples.toFloat()).coerceIn(0f, 1f)
        val tail = ((count - index - 1) / fadeSamples.toFloat()).coerceIn(0f, 1f)
        return minOf(head, tail)
    }

    private companion object {
        const val SAMPLE_RATE = 22_050
        const val BYTES_PER_SAMPLE = 2
        val TWO_PI = (PI * 2.0).toFloat()
    }
}
