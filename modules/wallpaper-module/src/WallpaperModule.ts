import { NativeModule, requireNativeModule } from 'expo';

import { WallpaperModuleEvents } from './WallpaperModule.types';

declare class WallpaperModule extends NativeModule<WallpaperModuleEvents> {
  setVideoPath(path: string): void;
  setAsWallpaper(): void;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<WallpaperModule>('WallpaperModule');
