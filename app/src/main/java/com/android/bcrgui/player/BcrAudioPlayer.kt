package com.android.bcrgui.player

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BcrAudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed

    private var activeUri: Uri? = null

    private val updateProgressAction = object : Runnable {
        override fun run() {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    _currentPosition.value = mp.currentPosition.toLong()
                    handler.postDelayed(this, 100)
                }
            }
        }
    }

    fun play(uri: Uri) {
        if (activeUri == uri && mediaPlayer != null) {
            resume()
            return
        }

        stop()
        activeUri = uri

        try {
            val mp = MediaPlayer().apply {
                setDataSource(context, uri)
                prepare()
                
                // Set playback speed if supported
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        playbackParams = PlaybackParams().apply { speed = _playbackSpeed.value }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPosition.value = _duration.value
                    handler.removeCallbacks(updateProgressAction)
                }
            }

            mediaPlayer = mp
            mp.start()

            _isPlaying.value = true
            _duration.value = mp.duration.toLong()
            _currentPosition.value = 0L

            handler.post(updateProgressAction)
        } catch (e: Exception) {
            e.printStackTrace()
            _isPlaying.value = false
        }
    }

    fun pause() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                _isPlaying.value = false
                handler.removeCallbacks(updateProgressAction)
            }
        }
    }

    fun resume() {
        mediaPlayer?.let { mp ->
            if (!mp.isPlaying) {
                mp.start()
                _isPlaying.value = true
                handler.post(updateProgressAction)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { mp ->
            mp.seekTo(positionMs.toInt())
            _currentPosition.value = positionMs
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        mediaPlayer?.let { mp ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val wasPlaying = mp.isPlaying
                    mp.playbackParams = mp.playbackParams.apply { this.speed = speed }
                    if (!wasPlaying) {
                        // In Android, setting playbackParams on paused player can start it; force pause it if it was paused
                        mp.pause()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun stop() {
        handler.removeCallbacks(updateProgressAction)
        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaPlayer = null
        _isPlaying.value = false
        _currentPosition.value = 0L
        activeUri = null
    }

    fun release() {
        stop()
    }
}
