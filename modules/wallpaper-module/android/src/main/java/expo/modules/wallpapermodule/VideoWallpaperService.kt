package expo.modules.wallpapermodule

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.io.File

class VideoWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine {
        return VideoEngine()
    }

    inner class VideoEngine : Engine() {
        private var exoPlayer: ExoPlayer? = null
        private var keyguardManager: KeyguardManager? = null
        private var hasPlayedOnce: Boolean = false
        private var isPrepared: Boolean = false

        private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == "video_path") {
                Log.d("WallpaperService", "Video path changed, reloading...")
                loadVideo()
            }
        }

        private val wallpaperReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_USER_PRESENT -> {
                        // System unlocked
                        exoPlayer?.let { player ->
                            try {
                                if (player.isPlaying) {
                                    // Let it finish naturally
                                } else if (hasPlayedOnce) {
                                    // Already finished, stay at end
                                    val duration = player.duration
                                    if (duration > 0) player.seekTo(duration)
                                }
                            } catch (e: Exception) {
                                Log.e("WallpaperService", "Error in USER_PRESENT", e)
                            }
                        }
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        Log.d("WallpaperService", "Screen turned off: Resetting state")
                        hasPlayedOnce = false
                        try {
                            if (exoPlayer?.isPlaying == true) {
                                exoPlayer?.pause()
                            }
                        } catch (e: Exception) {
                            Log.e("WallpaperService", "Error in SCREEN_OFF", e)
                        }
                    }
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            
            keyguardManager = this@VideoWallpaperService.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                this@VideoWallpaperService.registerReceiver(wallpaperReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                this@VideoWallpaperService.registerReceiver(wallpaperReceiver, filter)
            }
            
            val prefs = this@VideoWallpaperService.getSharedPreferences("WallpaperPrefs", Context.MODE_PRIVATE)
            prefs.registerOnSharedPreferenceChangeListener(prefsListener)
            
            loadVideo()
        }

        private fun loadVideo() {
            val prefs = this@VideoWallpaperService.getSharedPreferences("WallpaperPrefs", Context.MODE_PRIVATE)
            val videoPath = prefs.getString("video_path", null)
            
            if (videoPath != null) {
                try {
                    isPrepared = false
                    exoPlayer?.release()
                    
                    exoPlayer = ExoPlayer.Builder(this@VideoWallpaperService).build().apply {
                        val mediaItem = MediaItem.fromUri(Uri.fromFile(File(videoPath)))
                        setMediaItem(mediaItem)
                        repeatMode = Player.REPEAT_MODE_OFF
                        volume = 0f
                        
                        addListener(object : Player.Listener {
                            override fun onPlaybackStateChanged(playbackState: Int) {
                                if (playbackState == Player.STATE_READY) {
                                    isPrepared = true
                                    Log.d("WallpaperService", "ExoPlayer ready")
                                    if (isVisible) {
                                        handleVisibilityChange(true)
                                    }
                                } else if (playbackState == Player.STATE_ENDED) {
                                    pause()
                                    val duration = duration
                                    if (duration > 0) seekTo(duration)
                                }
                            }

                            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                                Log.e("WallpaperService", "ExoPlayer Error: ${error.message}", error)
                            }
                        })
                        
                        prepare()
                    }
                    
                    // Re-link surface if already created
                    if (surfaceHolder?.surface != null) {
                        exoPlayer?.setVideoSurface(surfaceHolder.surface)
                    }
                } catch (e: Exception) {
                    Log.e("WallpaperService", "Error loading video with ExoPlayer", e)
                }
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            exoPlayer?.setVideoSurface(holder.surface)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            handleVisibilityChange(visible)
        }

        private fun handleVisibilityChange(visible: Boolean) {
            if (visible) {
                if (!isPrepared) {
                    Log.d("WallpaperService", "Visible but not prepared yet, waiting...")
                    return
                }

                val isLocked = keyguardManager?.isKeyguardLocked == true
                
                if (isLocked) {
                    // Woke up to Lock Screen
                    exoPlayer?.let { player ->
                        try {
                            if (!hasPlayedOnce) {
                                Log.d("WallpaperService", "Starting playback on Lock Screen")
                                player.seekTo(0)
                                player.play()
                                hasPlayedOnce = true
                            } else {
                                Log.d("WallpaperService", "Already played once, skipping start")
                            }
                        } catch (e: Exception) {
                            Log.e("WallpaperService", "Error in Lock Screen visibility", e)
                        }
                    }
                } else {
                    // Home Screen visible
                    exoPlayer?.let { player ->
                        try {
                            if (player.isPlaying) {
                                Log.d("WallpaperService", "Home screen visible: Animation continuing smoothly")
                            } else if (!hasPlayedOnce) {
                                Log.d("WallpaperService", "Direct Home Wake: Seeking to end")
                                val duration = player.duration
                                if (duration > 0) player.seekTo(duration)
                                hasPlayedOnce = true
                            }
                        } catch (e: Exception) {
                            Log.e("WallpaperService", "Error in Home Screen visibility", e)
                        }
                    }
                }
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            try {
                exoPlayer?.clearVideoSurface()
            } catch (e: Exception) {
                Log.e("WallpaperService", "Error in onSurfaceDestroyed", e)
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            try {
                this@VideoWallpaperService.unregisterReceiver(wallpaperReceiver)
                val prefs = this@VideoWallpaperService.getSharedPreferences("WallpaperPrefs", Context.MODE_PRIVATE)
                prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
            } catch (e: Exception) {
                Log.e("WallpaperService", "Error in onDestroy", e)
            }
            exoPlayer?.release()
            exoPlayer = null
        }
    }
}
