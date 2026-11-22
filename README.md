# SoundShelf

**Your Music. Your Library. Your Experience.**

SoundShelf is a native Android music player, local library manager, and modular streaming discovery platform built with **Kotlin**, **Jetpack Compose**, **Material Design 3**, and **AndroidX Media3**.

## Core Architecture

- **Offline-First Local Music Foundation** (`com.example.library`): Incremental `MediaStore` scanning, Storage Access Framework (`OpenDocument`) audio import, folder exclusions, duplicate detection, `AudioInspector` (`MediaExtractor` + `MediaMetadataRetriever`), and lossless-preserving tag editing with undo snapshots.
- **Media3 Background Playback Engine** (`com.example.playback`): Foreground `MediaSessionService` + `ExoPlayer`, lock-screen & Bluetooth media controls, audio focus & noisy-output auto-pause, `PlaybackResumption`, and device-accurate `AudioEffectsManager` (`Equalizer`, `BassBoost`, `LoudnessEnhancer`).
- **Modular Online Providers** (`com.example.providers`):
  - **Audius**: Public Discovery API integration with creator-permitted download checks.
  - **Jamendo**: Creative Commons API v3.0 integration with per-track license & download validation.
  - **YouTube Music (Experimental)**: Isolated, policy-compliant compatibility adapter handing off to the official YouTube Music app (zero credential scraping, DRM bypass, or unauthorized ripping).
  - **Spotify (External)**: Separate external-app handoff & Keystore-encrypted optional OAuth vault (never mixed into SoundShelf's cross-provider audio queue).
- **Lyrics Studio** (`com.example.lyrics`): Embedded lyrics, sidecar `.lrc` reading, SAF `.lrc` import, timing offset adjuster, inline timestamp stamping, and optional open LRCLIB lookup.
- **Download Finder** (`com.example.downloadfinder`): Locates legitimate creator-permitted downloads and official digital store pages (Qobuz, Bandcamp, 7digital), verifies direct downloads with SHA-256 + `AudioInspector`, and supports **Find a Better Version** safe library replacement.
- **Theme Studio** (`com.example.core.designsystem`): 15 distinct visual identities (Midnight Studio, Pure AMOLED, Liquid Glass, Glassmorphism, Editorial, Retro Vinyl, Cyberpunk, Synthwave, Minimal White, Warm Analog, Ocean, Neumorphism, Modern Broadcast, Pastel Sound, Monochrome) plus a Custom Theme Editor and 5 Now Playing visual layouts.

## Privacy & Offline Guarantee

- **Zero Ads, Zero Analytics, Zero Trackers**: No advertising SDKs or telemetry.
- **Local-Only Mode**: Toggle **Local-Only Offline Mode** in Settings to strictly block all online provider network requests while keeping 100% of local library, playlist, lyrics, equalizer, and metadata features active.
- **Encrypted Token Storage**: Any optional OAuth tokens are encrypted using Android Keystore (`AES-256-GCM`).

## Building via Gradle / GitHub Actions

See `.github/workflows/build-apk.yml` for automated debug APK builds and unit tests (`gradle :app:assembleDebug` and `gradle :app:testDebugUnitTest`).
