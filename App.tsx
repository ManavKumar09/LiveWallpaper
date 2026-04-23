import React, { useEffect, useRef, useState } from 'react';
import { 
  StyleSheet, 
  View, 
  AppState, 
  AppStateStatus, 
  Dimensions, 
  TouchableOpacity, 
  Text,
  Image,
  Animated
} from 'react-native';
import { Video, ResizeMode } from 'expo-av';
import * as ImagePicker from 'expo-image-picker';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { StatusBar } from 'expo-status-bar';

const { width, height } = Dimensions.get('window');

import WallpaperModule from './modules/wallpaper-module';

const VIDEO_KEY = '@user_video_uri';

export default function App() {
  const [videoUri, setVideoUri] = useState<string | null>(null);
  const fadeAnim = useRef(new Animated.Value(0)).current;
  
  const videoRef = useRef<Video>(null);
  const appState = useRef(AppState.currentState);

  useEffect(() => {
    // Fade in UI on mount
    Animated.timing(fadeAnim, {
      toValue: 1,
      duration: 1000,
      useNativeDriver: true,
    }).start();

    loadSavedMedia();

    const handleAppStateChange = (nextAppState: AppStateStatus) => {
      if (
        appState.current.match(/inactive|background/) &&
        nextAppState === 'active'
      ) {
        // Replay on wake
        if (videoRef.current) {
          videoRef.current.replayAsync();
        }
      }
      appState.current = nextAppState;
    };

    const subscription = AppState.addEventListener('change', handleAppStateChange);
    return () => subscription.remove();
  }, []);

  const loadSavedMedia = async () => {
    try {
      const savedVideo = await AsyncStorage.getItem(VIDEO_KEY);
      if (savedVideo) {
        setVideoUri(savedVideo);
        WallpaperModule.setVideoPath(savedVideo);
      }
    } catch (e) {
      console.error('Failed to load media', e);
    }
  };

  const pickVideo = async () => {
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ['videos'],
      allowsEditing: true,
      quality: 1,
    });

    if (!result.canceled) {
      const uri = result.assets[0].uri;
      setVideoUri(uri);
      await AsyncStorage.setItem(VIDEO_KEY, uri);
      
      // Update Native Module
      WallpaperModule.setVideoPath(uri);
    }
  };

  const setAsSystemWallpaper = () => {
    if (videoUri) {
      WallpaperModule.setAsWallpaper();
    }
  };

  return (
    <View style={styles.container}>
      <StatusBar style="light" translucent />

      {/* Background Layer */}
      {videoUri ? (
        <Video
          ref={videoRef}
          source={{ uri: videoUri }}
          style={styles.fullscreen}
          resizeMode={ResizeMode.COVER}
          shouldPlay={true}
          isLooping={false}
        />
      ) : (
        <View style={styles.emptyContainer}>
          <View style={styles.glowCircle} />
          <Image 
            source={require('./assets/logo.png')} 
            style={styles.logo} 
          />
          <Text style={styles.title}>Live Wallpaper Maker</Text>
          <Text style={styles.subtitle}>Bring your lock screen to life with seamless animations.</Text>
        </View>
      )}

      {/* Controls Panel */}
      <Animated.View style={[styles.controlsPanel, { opacity: fadeAnim }]}>
        {!videoUri ? (
           <TouchableOpacity style={[styles.primaryButton, styles.shadow]} onPress={pickVideo}>
             <Text style={styles.primaryButtonText}>Select Video from Gallery</Text>
           </TouchableOpacity>
        ) : (
          <>
            <View style={styles.actionRow}>
              <TouchableOpacity style={styles.secondaryButton} onPress={pickVideo}>
                <Text style={styles.secondaryButtonText}>Change Video</Text>
              </TouchableOpacity>
              
              <TouchableOpacity style={styles.secondaryButton} onPress={() => videoRef.current?.replayAsync()}>
                <Text style={styles.secondaryButtonText}>Replay Animation</Text>
              </TouchableOpacity>
            </View>
            
            <TouchableOpacity 
              style={[styles.primaryButton, styles.shadow, { marginTop: 15 }]} 
              onPress={setAsSystemWallpaper}
            >
              <Text style={styles.primaryButtonText}>✨ Set as Live Wallpaper</Text>
            </TouchableOpacity>
          </>
        )}
      </Animated.View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0A0A0C',
  },
  fullscreen: {
    width: width,
    height: height,
    position: 'absolute',
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 30,
  },
  glowCircle: {
    position: 'absolute',
    width: 300,
    height: 300,
    borderRadius: 150,
    backgroundColor: '#007AFF',
    opacity: 0.15,
    top: height * 0.2,
    transform: [{ scale: 1.5 }],
  },
  logo: {
    width: 140,
    height: 140,
    borderRadius: 35,
    marginBottom: 40,
    borderWidth: 2,
    borderColor: 'rgba(255,255,255,0.1)',
  },
  title: {
    fontSize: 32,
    fontWeight: '800',
    color: '#FFFFFF',
    textAlign: 'center',
    marginBottom: 12,
    letterSpacing: 0.5,
  },
  subtitle: {
    fontSize: 16,
    color: '#888890',
    textAlign: 'center',
    lineHeight: 24,
    marginBottom: 40,
  },
  controlsPanel: {
    position: 'absolute',
    bottom: 50,
    left: 24,
    right: 24,
    backgroundColor: 'rgba(20, 20, 22, 0.85)',
    borderRadius: 28,
    padding: 24,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.08)',
  },
  actionRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: 12,
  },
  primaryButton: {
    backgroundColor: '#007AFF',
    paddingVertical: 18,
    borderRadius: 20,
    alignItems: 'center',
    width: '100%',
  },
  primaryButtonText: {
    color: '#FFFFFF',
    fontSize: 17,
    fontWeight: '700',
    letterSpacing: 0.3,
  },
  secondaryButton: {
    flex: 1,
    backgroundColor: 'rgba(255,255,255,0.1)',
    paddingVertical: 16,
    borderRadius: 16,
    alignItems: 'center',
  },
  secondaryButtonText: {
    color: '#FFFFFF',
    fontSize: 15,
    fontWeight: '600',
  },
  shadow: {
    shadowColor: '#007AFF',
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.4,
    shadowRadius: 16,
    elevation: 10,
  },
});
