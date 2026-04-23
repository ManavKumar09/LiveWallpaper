// Reexport the native module. On web, it will be resolved to WallpaperModule.web.ts
// and on native platforms to WallpaperModule.ts
export { default } from './src/WallpaperModule';
export { default as WallpaperModuleView } from './src/WallpaperModuleView';
export * from  './src/WallpaperModule.types';
