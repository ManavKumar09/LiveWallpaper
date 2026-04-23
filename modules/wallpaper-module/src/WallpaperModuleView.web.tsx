import * as React from 'react';

import { WallpaperModuleViewProps } from './WallpaperModule.types';

export default function WallpaperModuleView(props: WallpaperModuleViewProps) {
  return (
    <div>
      <iframe
        style={{ flex: 1 }}
        src={props.url}
        onLoad={() => props.onLoad({ nativeEvent: { url: props.url } })}
      />
    </div>
  );
}
