# ✨ Live Wallpaper Maker

A premium Android Live Wallpaper application built with **React Native (Expo)** and **Native Android Kotlin**. Bring your lock screen to life with high-performance, smooth video animations.

![App Preview](https://raw.githubusercontent.com/ManavKumar09/LiveWallpaper/main/assets/preview.png)

## 🚀 Key Features
- **Ultra-Smooth Playback:** Powered by **Jetpack Media3 (ExoPlayer)** for seamless, stutter-free video animations.
- **Permanent Storage:** Videos are automatically backed up to the app's internal storage, ensuring your wallpaper never disappears (even after reboots or cache clears).
- **Modern UI:** A sleek, dark-themed interface designed with premium aesthetics and smooth transitions.
- **Battery Efficient:** Intelligent lifecycle management ensures the video only plays when the screen is visible.

## 📦 Download & Installation
To use the app on your Android device, you can download the pre-compiled APK:

### 1. Locate the APK
The latest production build is located at:
`android/app/build/outputs/apk/release/app-release.apk`

### 2. Installation Steps
1.  Transfer the `app-release.apk` to your Android device.
2.  Open the file on your device and select **Install** (you may need to allow "Installation from Unknown Sources").
3.  Open the app, select your favorite video, and tap **✨ Set as Live Wallpaper**.

## 🛠️ Tech Stack
- **Frontend:** React Native (Expo)
- **Native Module:** Kotlin (Android)
- **Video Engine:** Jetpack Media3 ExoPlayer
- **Storage:** SharedPreferences & Internal File Storage

## 👨‍💻 Development
If you want to build the project yourself:

1.  **Clone the repo:**
    ```bash
    git clone https://github.com/ManavKumar09/LiveWallpaper.git
    ```
2.  **Install dependencies:**
    ```bash
    npm install
    ```
3.  **Build the Release APK:**
    Ensure you have **JDK 21** installed and run:
    ```bash
    cd android
    ./gradlew assembleRelease
    ```

---
*Created with ❤️ by Manav*
