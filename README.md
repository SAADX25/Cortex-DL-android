<div align="center">

<br/>

# ⚡ Cortex DL — Android

### Advanced Video & Audio Downloader for Android

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-7F52FF?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose BOM](https://img.shields.io/badge/Jetpack%20Compose-2026.05.01-4285F4?style=flat&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Android SDK](https://img.shields.io/badge/SDK-24%20→%2037-3DDC84?style=flat&logo=android&logoColor=white)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-GPL--3.0-blue?style=flat&logo=gnu)](LICENSE)
[![yt-dlp](https://img.shields.io/badge/yt--dlp-2025.12.08-FF0000?style=flat&logo=youtube&logoColor=white)](https://github.com/yt-dlp/yt-dlp)
[![Gradle](https://img.shields.io/badge/Gradle-9.5.1-02303A?style=flat&logo=gradle)](https://gradle.org)

---

**Cortex DL** is a powerful, open-source video and audio downloader for Android.  
Built on top of [Seal](https://github.com/JunkFood02/Seal) by [JunkFood02](https://github.com/JunkFood02), enhanced with an exclusive **Gradient Dark Theme**, an **Auto-Update System**, and a completely modernized tech stack.

</div>

---

## 🧰 Tech Stack & Tools Used

> Full breakdown of every technology, library, and tool powering this project.

### 🖥️ Language & Compiler

| Tool | Version | Role |
|------|---------|------|
| **Kotlin** | `2.3.21` | Primary language — 100% Kotlin codebase |
| **K2 Compiler** | bundled with Kotlin 2.x | Next-gen Kotlin compiler for faster builds |
| **KSP** (Kotlin Symbol Processing) | latest | Annotation processing (Room, etc.) |
| **Java Toolchain** | `JDK 21` | Build-time JVM compatibility |

---

### 🏗️ Build System

| Tool | Version | Role |
|------|---------|------|
| **Gradle** | `9.5.1` | Build automation system |
| **Android Gradle Plugin (AGP)** | `9.2.1` | Android-specific Gradle integration |
| **Gradle KTS** | — | Kotlin DSL for all build scripts |
| **buildSrc** | — | Centralized version catalog & build logic |
| **Foojay Toolchain Resolver** | `1.0.0` | Automatic JDK provisioning |

---

### 📱 Android SDK

| Setting | Value |
|---------|-------|
| **Compile SDK** | API 37 (Android 17) |
| **Target SDK** | API 37 (Android 17) |
| **Min SDK** | API 24 (Android 7.0 Nougat) |
| **Application ID** | `com.cortex.dl` |
| **Namespace** | `com.cortex.dl` |

---

### 🎨 UI Framework

| Library | Version | Role |
|---------|---------|------|
| **Jetpack Compose BOM** | `2026.05.01` | Unified Compose versioning |
| **Compose UI** | BOM-managed | Core UI rendering |
| **Compose Foundation** | BOM-managed | Layout primitives |
| **Compose Material 3** | `1.3.1 stable` | Material Design 3 component library |
| **Compose Animation** | BOM-managed | Smooth transitions & animations |
| **Compose Navigation** | BOM-managed | In-app navigation |
| **Compose UI Tooling** | BOM-managed | Preview & debugging |
| **Lifecycle Runtime Compose** | latest | Lifecycle-aware Compose integration |
| **Core SplashScreen** | `1.0.1` | Android 12+ splash screen API |

---

### 🗄️ Data Persistence

| Library | Version | Role |
|---------|---------|------|
| **Room Runtime** | `2.8.4` | SQLite ORM for download history |
| **Room KTX** | `2.8.4` | Coroutines extensions for Room |
| **Room Compiler (KSP)** | `2.8.4` | Code generation for DAO/Entity |
| **MMKV** | latest | High-performance key-value storage (preferences) |

---

### 🌐 Networking

| Library | Version | Role |
|---------|---------|------|
| **OkHttp** | `4.12.0` | HTTP client for update checks & networking |
| **Coil 3** | `3.4.0` | Image loading (thumbnail previews) |
| **Coil OkHttp** | `3.4.0` | OkHttp network backend for Coil |

---

### 📦 Dependency Injection

| Library | Version | Role |
|---------|---------|------|
| **Koin Android** | `4.2.1` | Dependency injection framework |
| **Koin Compose** | `4.2.1` | Compose integration for Koin |

---

### ⚙️ Async & Concurrency

| Library | Version | Role |
|---------|---------|------|
| **Kotlin Coroutines** | `1.11.0` | Asynchronous programming model |
| **Kotlinx Serialization JSON** | latest | JSON parsing/serialization |

---

### 🎬 Downloader Core

| Library | Version | Role |
|---------|---------|------|
| **yt-dlp-android** | `2025.12.08` | Core download engine (yt-dlp wrapper) |
| **aria2c** | bundled | Multi-connection parallel downloader |
| **mutagen** | bundled | Audio metadata embedding |
| **AndroidX WebKit** | `1.16.0` | WebView anti-bot bypass (Meta login) |
| **AndroidX DocumentFile** | latest | SAF-based file access |

---

### 🔧 Developer Tooling

| Tool | Role |
|------|------|
| **Android Studio Ladybug** | Official IDE |
| **ktfmt (Gradle Plugin)** | Kotlin code formatter (kotlinLangStyle) |
| **ProGuard / R8** | Code shrinking & obfuscation (release builds) |
| **Lint** | Static analysis (MissingTranslation/ExtraTranslation disabled) |
| **JUnit 4** | Unit testing framework |
| **AndroidX Test Ext** | Android instrumented testing |
| **Espresso Core** | UI testing framework |

---

### 🏛️ Architecture

| Pattern | Implementation |
|---------|----------------|
| **Architecture** | MVVM + Clean Architecture |
| **UI Layer** | Jetpack Compose (single-activity, no fragments) |
| **State Management** | `StateFlow` + `ViewModel` |
| **Navigation** | Compose Navigation |
| **Background Work** | Android `Service` + Kotlin Coroutines |
| **Theme System** | Material 3 dynamic colors + custom Gradient Dark theme |

---

## 📁 Project Structure

```
Cortex-DL-android/
│
├── 📄 build.gradle.kts              ← Root Gradle build script
├── 📄 settings.gradle.kts           ← Project settings & module includes
├── 📄 gradle.properties             ← Gradle JVM args & project properties
├── 📄 gradlew / gradlew.bat         ← Gradle wrapper scripts
├── 📄 .gitignore                    ← Git ignore rules
├── 📄 LICENSE                       ← GPL-3.0 license
│
├── 📂 buildSrc/                     ← Centralized build logic
│   └── 📂 src/
│       └── version constants, dependency versions
│
├── 📂 gradle/
│   └── 📄 libs.versions.toml        ← Version catalog (all dependency versions)
│
├── 📂 color/                        ← Standalone color library module
│
└── 📂 app/                          ← Main application module
    │
    ├── 📄 build.gradle.kts          ← App-level build config (SDK, deps, flavors)
    ├── 📄 proguard-rules.pro        ← ProGuard rules for release builds
    │
    ├── 📂 schemas/                  ← Room database migration schemas (JSON)
    │
    └── 📂 src/main/
        │
        ├── 📄 AndroidManifest.xml   ← App manifest (permissions, activities)
        │
        ├── 📂 res/                  ← Android resources
        │   ├── drawable/            ← Icons & vector assets
        │   ├── values/              ← Strings, colors, themes
        │   └── mipmap/              ← App launcher icons
        │
        └── 📂 java/com/cortex/dl/
            │
            ├── 📄 App.kt                    ← Application class (Koin init)
            ├── 📄 MainActivity.kt           ← Single entry-point Activity
            ├── 📄 DownloadService.kt        ← Background download service
            ├── 📄 CrashReportActivity.kt    ← Crash handling & reporting
            ├── 📄 NotificationActionReceiver.kt ← Download notification actions
            │
            ├── 📂 database/                 ← Room database layer
            │   ├── 📄 AppDatabase.kt        ← Room database definition
            │   ├── 📄 VideoInfoDao.kt       ← DAO for download history
            │   ├── 📂 backup/
            │   │   ├── 📄 Backup.kt         ← Backup data model
            │   │   └── 📄 BackupUtil.kt     ← Import/export logic
            │   └── 📂 objects/
            │       ├── 📄 CommandTemplate.kt    ← Custom yt-dlp templates
            │       ├── 📄 CookieProfile.kt      ← Cookie authentication profiles
            │       ├── 📄 DownloadedVideoInfo.kt ← Download record entity
            │       └── 📄 OptionShortcut.kt     ← Download option presets
            │
            ├── 📂 download/                 ← Download execution layer
            │   ├── 📄 DownloaderV2.kt       ← Core download engine wrapper
            │   ├── 📄 Task.kt               ← Download task model
            │   └── 📄 TaskFactory.kt        ← Task creation & configuration
            │
            ├── 📂 ui/                       ← Jetpack Compose UI layer
            │   ├── 📄 AppNavigation.kt      ← Navigation graph & routes
            │   ├── 📄 MainScreen.kt         ← Home screen (URL input)
            │   ├── 📄 MainViewModel.kt      ← ViewModel for main screen
            │   ├── 📄 DownloadsHistoryScreen.kt ← Download history list
            │   ├── 📄 SettingsScreen.kt     ← App settings UI
            │   ├── 📄 VideoInfoBottomSheet.kt ← Format selection bottom sheet
            │   └── 📂 theme/                ← Design system
            │       ├── 📄 Theme.kt          ← Material 3 theme entry point
            │       ├── 📄 Color.kt          ← Color definitions
            │       ├── 📄 ColorScheme.kt    ← Light/Dark color schemes
            │       ├── 📄 GradientDarkTheme.kt ← Exclusive gradient dark theme
            │       ├── 📄 Shape.kt          ← Shape tokens
            │       └── 📄 Type.kt           ← Typography tokens
            │
            └── 📂 util/                     ← Utility & helper layer
                ├── 📄 Cookie.kt             ← Cookie parsing (Netscape/JSON/Header)
                ├── 📄 DatabaseUtil.kt       ← DB helper operations
                ├── 📄 DateTimeUtil.kt       ← Date/time formatting
                ├── 📄 DownloadUtil.kt       ← yt-dlp command builder
                ├── 📄 FileUtil.kt           ← File path & SAF utilities
                ├── 📄 FormatValidator.kt    ← Format/quality filter logic
                ├── 📄 NotificationUtil.kt   ← Notification channel & builder
                ├── 📄 PreferenceUtil.kt     ← MMKV preference wrapper
                ├── 📄 ProxyManager.kt       ← Proxy configuration manager
                ├── 📄 ProxyValidator.kt     ← Proxy validation & testing
                ├── 📄 TextUtil.kt           ← String & text helpers
                ├── 📄 UpdateUtil.kt         ← Auto-update checker (GitHub API)
                └── 📄 VideoInfo.kt          ← Video metadata model
```

---

## 🚀 Getting Started (Developer Setup)

### Prerequisites

| Requirement | Version |
|-------------|---------|
| **Android Studio** | Ladybug or later |
| **JDK** | 21 (auto-provisioned via Foojay) |
| **Android SDK** | API 24 → 37 |
| **Gradle** | 9.5.1 (via wrapper) |

### Clone & Build

```bash
# Clone the repository
git clone https://github.com/SAADX25/Cortex-DL-android.git
cd Cortex-DL-android

# Debug build
./gradlew assembleDebug

# Release build (requires keystore.properties)
./gradlew assembleRelease

# Run tests
./gradlew test
./gradlew connectedAndroidTest

# Clean build
./gradlew clean
```

### Output APKs

By default, the build produces **ABI-split APKs** in:

```
app/build/outputs/apk/generic/release/
├── CortexDL-<version>-arm64-v8a.apk     ← Recommended (most devices)
├── CortexDL-<version>-armeabi-v7a.apk   ← Older 32-bit ARM
├── CortexDL-<version>-x86_64.apk        ← Intel/AMD 64-bit
├── CortexDL-<version>-x86.apk           ← Older Intel/AMD
└── CortexDL-<version>-universal.apk     ← All architectures
```

### Build Variants

| Variant | Application ID | Description |
|---------|---------------|-------------|
| `genericDebug` | `com.cortex.dl.debug` | Development build with debug suffix |
| `genericRelease` | `com.cortex.dl` | Production build with minification |

---

## 🏛️ Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                    UI Layer                         │
│         Jetpack Compose · Material 3                │
│   MainScreen · SettingsScreen · HistoryScreen       │
│         BottomSheets · Navigation                   │
└───────────────────┬─────────────────────────────────┘
                    │ StateFlow / collectAsState
┌───────────────────▼─────────────────────────────────┐
│                ViewModel Layer                      │
│            MainViewModel (Koin DI)                  │
│        Kotlin Coroutines · StateFlow                │
└───────────────────┬─────────────────────────────────┘
                    │
        ┌───────────┴────────────┐
        │                        │
┌───────▼──────────┐   ┌────────▼────────────────────┐
│  Download Layer  │   │      Data Layer              │
│  DownloaderV2    │   │  Room DB · MMKV · Backup     │
│  Task / Factory  │   │  VideoInfoDao · Preferences  │
│  yt-dlp · aria2c │   └──────────────────────────────┘
└──────────────────┘
        │
┌───────▼──────────┐
│  Android Service │
│  DownloadService │
│  Notifications   │
└──────────────────┘
```

---

## 🔑 Key Features

- 🎨 **Exclusive Gradient Dark Theme** — Glassmorphism with deep OLED backgrounds & vibrant gradients
- ⚡ **Auto-Update System** — One-click in-app APK updates via GitHub Releases API
- 🌐 **1000+ Sites** — Powered by [yt-dlp](https://github.com/yt-dlp/yt-dlp)
- 🚀 **Parallel Downloads** — Embedded [aria2c](https://github.com/aria2/aria2) with multi-connection support
- 🍪 **Cookie Management** — Netscape / JSON / Header format with anti-bot bypass for Meta
- 🔐 **App Lock** — PIN & Biometric authentication with PBKDF2 hashing
- 📄 **Download Docs** — Save video metadata as `.txt` alongside downloads
- 🎬 **Format Selection** — MP4-only filter, implausible size detection, quality validation
- 📋 **Playlist Support** — Batch download with progress tracking per video

---

## 🤝 Contributing

1. **Fork** this repository
2. **Create** a feature branch: `git checkout -b feature/your-feature`
3. **Commit** with clear messages: `git commit -m "feat: add amazing feature"`
4. **Push** to your fork: `git push origin feature/your-feature`
5. **Open** a Pull Request with a detailed description

> Please follow the existing code style — `ktfmt` with `kotlinLangStyle()` is enforced automatically.

---

## 📜 License

This project is licensed under the **GNU General Public License v3.0**.  
See [LICENSE](LICENSE) for the full text.

---

## 🙏 Credits

| Project | Author | Role |
|---------|--------|------|
| [Seal](https://github.com/JunkFood02/Seal) | JunkFood02 | Base application |
| [yt-dlp](https://github.com/yt-dlp/yt-dlp) | yt-dlp team | Download engine |
| [aria2](https://github.com/aria2/aria2) | aria2 team | Parallel downloader |
| [mutagen](https://github.com/quodlibet/mutagen) | quodlibet | Audio metadata |
| [Koin](https://github.com/InsertKoinIO/koin) | InsertKoinIO | Dependency injection |
| [Coil](https://github.com/coil-kt/coil) | coil-kt | Image loading |
| [MMKV](https://github.com/Tencent/MMKV) | Tencent | Key-value storage |

---

<div align="center">

Made with ❤️ · GPL-3.0 Licensed · Built with Kotlin & Jetpack Compose

</div>
