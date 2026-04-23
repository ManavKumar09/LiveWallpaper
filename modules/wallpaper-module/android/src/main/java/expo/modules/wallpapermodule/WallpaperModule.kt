package expo.modules.wallpapermodule

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class WallpaperModule : Module() {
  override fun definition() = ModuleDefinition {
    // The name of the module in JavaScript.
    Name("WallpaperModule")

    Function("setVideoPath") { path: String ->
      appContext.reactContext?.let { context ->
        val prefs = context.getSharedPreferences("WallpaperPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("video_path", path).apply()
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
