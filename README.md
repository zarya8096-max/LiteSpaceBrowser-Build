# LiteSpace Browser — Android Lite Apps & Sandbox Browser

**LiteSpace Browser** is a complete, production-ready Android application written in **Kotlin** with **Jetpack Compose + Material 3**, **MVVM + Clean Architecture**, **Room**, **DataStore Preferences**, **WorkManager**, and **AndroidX WebKit**.

## Key Capabilities

1. **Core Lite Apps Engine**: Convert any website into an independent Lite App with its own custom name, monogram/color icon, start URL, orientation lock, display mode (`Standalone`, `Frameless`, `Fullscreen Immersive`), and isolated storage profile.
2. **Home Screen Shortcuts & Multi-Document Recents**: Pin any Lite App to the Android Home Screen via `ShortcutManagerCompat`. Each Lite App launches into `LiteAppActivity` with `FLAG_ACTIVITY_NEW_DOCUMENT | FLAG_ACTIVITY_MULTIPLE_TASK` and a custom `ActivityManager.TaskDescription` so each Lite App appears as a separate native card in Android Recents.
3. **Advanced WebView Controls**: Per-app JavaScript toggle, Desktop/Mobile User-Agent presets & custom strings, Custom CSS & JavaScript (`UserScript`) injection (`onPageStarted` or `onPageFinished` + live in-browser injector), Force Dark Web mode, Reader Mode (`reader_mode.js`), Find-in-Page, Text Zoom & Page Zoom sliders, File Uploads & `DownloadManager` integration, Fullscreen HTML5 video playback, Pull-to-Refresh, and Edge Swipe navigation gestures.
4. **Privacy & Security Shield**: Built-in EasyList/AdGuard Content Blocker (`ContentBlockerEngine` with 90+ ad/tracker domains + custom Room filter & `@@` exception rules), Per-Profile Cookie & `localStorage` Vault Isolation (`ProfileIsolationManager` + `CookieVaultDao`), Third-Party Cookie blocking, Automatic HTTPS upgrade, Clear-on-Exit, Incognito sessions, and Biometric (`BiometricPrompt`) + SHA-256 PIN lock.
5. **Web Notifications & RSS/Atom Feed Monitor**: W3C `window.Notification` JS bridge (`LiteSpaceNotificationBridge`) for active pages + battery-efficient background RSS/Atom feed polling via `WorkManager` (`FeedNotificationWorker`).
6. **Bilingual UI (English + Persian RTL)**: Complete Material You dynamic color theming, Light/Dark/AMOLED modes, and instant switching between English (`LTR`) and Persian (`فارسی - RTL`).

## Quick Build Commands

```bash
# Build Debug APK
./gradlew assembleDebug

# Run Unit Tests
./gradlew testDebugUnitTest

# Build Signed/Minified Release APK
./gradlew assembleRelease
```
