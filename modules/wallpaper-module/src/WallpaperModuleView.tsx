import { requireNativeView } from 'expo';
import * as React from 'react';

import { WallpaperModuleViewProps } from './WallpaperModule.types';

const NativeView: React.ComponentType<WallpaperModuleViewProps> =
  requireNativeView('WallpaperModule');

export default function WallpaperModuleView(props: WallpaperModuleViewProps) {
  return <NativeView {...props} />;
}
