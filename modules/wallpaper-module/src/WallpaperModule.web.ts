import { registerWebModule, NativeModule } from 'expo';

import { ChangeEventPayload } from './WallpaperModule.types';

type WallpaperModuleEvents = {
  onChange: (params: ChangeEventPayload) => void;
}

class WallpaperModule extends NativeModule<WallpaperModuleEvents> {
  PI = Math.PI;
  async setValueAsync(value: string): Promise<void> {
    this.emit('onChange', { value });
  }
  hello() {
    return 'Hello world! 👋';
  }
};

export default registerWebModule(WallpaperModule, 'WallpaperModule');
