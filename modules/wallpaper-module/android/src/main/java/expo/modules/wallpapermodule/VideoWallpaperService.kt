package expo.modules.wallpapermodule

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.net.Uri
import android.content.SharedPreferences
import android.util.Log
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder

class VideoWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine {
        return VideoEngine()
    }

    inner class VideoEngine : Engine() {
        private var mediaPlayer: MediaPlayer? = null
        private var keyguardManager: KeyguardManager? = null
        private var wasVisible: Boolean = false
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
                        mediaPlayer?.let { mp ->
                            try {
                                if (mp.isPlaying) {
                                    // Let it finish naturally
                                } else if (hasPlayedOnce) {
                                    // Already finished, stay at end
                                    val duration = mp.duration
                                    if (duration > 0) mp.seekTo(duration)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        // Reset state only when screen actually turns off
                        Log.d("WallpaperService", "Screen turned off: Resetting hasPlayedOnce")
                        hasPlayedOnce = false
                        try {
                            if (mediaPlayer?.isPlaying == true) {
                                mediaPlayer?.pause()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
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
            this@VideoWallpaperService.registerReceiver(wallpaperReceiver, filter)
            
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
                    mediaPlayer?.release()
                    mediaPlayer = MediaPlayer()
                    mediaPlayer?.setDataSource(this@VideoWallpaperService.applicationContext, Uri.parse(videoPath))
                    mediaPlayer?.isLooping = false 
                    mediaPlayer?.setVolume(0f, 0f)
                    
                    mediaPlayer?.setOnCompletionListener { mp ->
                        try {
                            mp.pause()
                            val duration = mp.duration
                            if (duration > 0) mp.seekTo(duration)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    mediaPlayer?.setOnPreparedListener {
                        isPrepared = true
                        Log.d("WallpaperService", "MediaPlayer prepared and ready")
                        // If it became visible while preparing, trigger it now
                        if (isVisible) {
                            handleVisibilityChange(true)
                        }
                    }
                    
                    mediaPlayer?.prepareAsync()
                    
                    // Re-link surface if already created
                    if (surfaceHolder?.surface != null) {
                        mediaPlayer?.setSurface(surfaceHolder.surface)
                    }
                } catch (e: Exception) {
                    Log.e("WallpaperService", "Error loading video", e)
                }
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            mediaPlayer?.setSurface(holder.surface)
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
                    mediaPlayer?.let { mp ->
                        try {
                            if (!hasPlayedOnce) {
                                Log.d("WallpaperService", "Starting playback on Lock Screen")
                                mp.seekTo(0)
                                mp.start()
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
                    mediaPlayer?.let { mp ->
                        try {
                            if (mp.isPlaying) {
                                Log.d("WallpaperService", "Home screen visible: Animation continuing smoothly")
                            } else if (!hasPlayedOnce) {
                                Log.d("WallpaperService", "Direct Home Wake: Seeking to end")
                                val duration = mp.duration
                                if (duration > 0) mp.seekTo(duration)
                                hasPlayedOnce = true
                            }
                        } catch (e: Exception) {
                            Log.e("WallpaperService", "Error in Home Screen visibility", e)
                        }
                    }
                }
            }
            wasVisible = visible
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            try {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                }
                mediaPlayer?.setSurface(null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            try {
                this@VideoWallpaperService.unregisterReceiver(wallpaperReceiver)
                val prefs = this@VideoWallpaperService.getSharedPreferences("WallpaperPrefs", Context.MODE_PRIVATE)
                prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }
}
