package expo.modules.wallpapermodule

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.net.Uri
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

        private val unlockReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_USER_PRESENT) {
                    mediaPlayer?.let { mp ->
                        try {
                            if (mp.isPlaying) {
                                // If it's already playing (from lock screen), let it finish smoothly
                            } else {
                                // If it wasn't playing, ensure it's at the end frame
                                val duration = mp.duration
                                if (duration > 0) {
                                    mp.seekTo(duration)
                                }
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
            val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
            this@VideoWallpaperService.registerReceiver(unlockReceiver, filter)
            
            val prefs = this@VideoWallpaperService.getSharedPreferences("WallpaperPrefs", Context.MODE_PRIVATE)
            val videoPath = prefs.getString("video_path", null)
            
            if (videoPath != null) {
                try {
                    mediaPlayer = MediaPlayer()
                    mediaPlayer?.setDataSource(this@VideoWallpaperService.applicationContext, Uri.parse(videoPath))
                    mediaPlayer?.isLooping = false 
                    mediaPlayer?.setVolume(0f, 0f)
                    
                    // Explicitly freeze on the last frame when the video naturally finishes
                    mediaPlayer?.setOnCompletionListener { mp ->
                        try {
                            mp.pause()
                            val duration = mp.duration
                            if (duration > 0) {
                                mp.seekTo(duration)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    
                    mediaPlayer?.prepare()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            mediaPlayer?.setSurface(holder.surface)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible && !wasVisible) {
                val isLocked = keyguardManager?.isKeyguardLocked == true
                
                if (isLocked) {
                    // Woke up to Lock Screen
                    mediaPlayer?.let { mp ->
                        try {
                            if (!hasPlayedOnce) {
                                mp.seekTo(0)
                                mp.start()
                                hasPlayedOnce = true
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } else {
                    // Woke up directly to Home Screen
                    mediaPlayer?.let { mp ->
                        try {
                            if (mp.isPlaying) mp.pause()
                            val duration = mp.duration
                            if (duration > 0) mp.seekTo(duration)
                            hasPlayedOnce = true
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } else if (!visible) {
                // Screen turned off or app covered
                try {
                    if (mediaPlayer?.isPlaying == true) {
                        mediaPlayer?.pause()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                hasPlayedOnce = false // Reset so it plays on next wake
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
                this@VideoWallpaperService.unregisterReceiver(unlockReceiver)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }
}
