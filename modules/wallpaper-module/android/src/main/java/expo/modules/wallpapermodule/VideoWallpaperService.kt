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

        private val unlockReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_USER_PRESENT) {
                    // User just unlocked - stop animation and freeze at end
                    val duration = mediaPlayer?.duration ?: 0
                    if (duration > 0) {
                        mediaPlayer?.seekTo(duration)
                    }
                    if (mediaPlayer?.isPlaying == true) {
                        mediaPlayer?.pause()
                    }
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            
            keyguardManager = this@VideoWallpaperService.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
            this@VideoWallpaperService.registerReceiver(unlockReceiver, filter)
            
            // Get path from SharedPreferences
            val prefs = this@VideoWallpaperService.getSharedPreferences("WallpaperPrefs", Context.MODE_PRIVATE)
            val videoPath = prefs.getString("video_path", null)
            
            if (videoPath != null) {
                try {
                    mediaPlayer = MediaPlayer()
                    mediaPlayer?.setDataSource(this@VideoWallpaperService.applicationContext, Uri.parse(videoPath))
                    mediaPlayer?.isLooping = false // Freeze at the end
                    mediaPlayer?.setVolume(0f, 0f) // Mute wallpaper
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
            if (visible) {
                val isLocked = keyguardManager?.isKeyguardLocked == true
                if (isLocked) {
                    // Lock screen: Play from beginning
                    mediaPlayer?.seekTo(0)
                    mediaPlayer?.start()
                } else {
                    // Home screen: Freeze at the end
                    val duration = mediaPlayer?.duration ?: 0
                    if (duration > 0) {
                        mediaPlayer?.seekTo(duration)
                    }
                    if (mediaPlayer?.isPlaying == true) {
                        mediaPlayer?.pause()
                    }
                }
            } else {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                }
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
            mediaPlayer?.setSurface(null)
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
