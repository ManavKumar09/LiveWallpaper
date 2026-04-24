package expo.modules.wallpapermodule

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import java.io.File

class WallpaperModule : Module() {
  override fun definition() = ModuleDefinition {
    // The name of the module in JavaScript.
    Name("WallpaperModule")

    Function("setVideoPath") { path: String ->
      appContext.reactContext?.let { context ->
        try {
          val uri = Uri.parse(path)
          val inputStream = context.contentResolver.openInputStream(uri)
          val outputFile = File(context.filesDir, "active_wallpaper.mp4")
          
          inputStream?.use { input ->
            outputFile.outputStream().use { output ->
              input.copyTo(output)
            }
          }
          
          val prefs = context.getSharedPreferences("WallpaperPrefs", Context.MODE_PRIVATE)
          prefs.edit().putString("video_path", outputFile.absolutePath).apply()
          Log.d("WallpaperModule", "Video saved permanently to: ${outputFile.absolutePath}")
        } catch (e: Exception) {
          Log.e("WallpaperModule", "Failed to save video permanently", e)
          // Fallback to original path if copy fails
          val prefs = context.getSharedPreferences("WallpaperPrefs", Context.MODE_PRIVATE)
          prefs.edit().putString("video_path", path).apply()
        }
      }
    }

    Function("setAsWallpaper") {
      appContext.reactContext?.let { context ->
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
        intent.putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(context, VideoWallpaperService::class.java)
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) 
        context.startActivity(intent)
      }
    }
  }
}
