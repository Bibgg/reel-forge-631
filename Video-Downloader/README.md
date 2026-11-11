# Video Downloader engine

This Android library module contains the download engines used by the Video Downloader app:

- NewPipe Extractor v0.27.1 for its supported services, including YouTube, SoundCloud, media.ccc.de, PeerTube (no P2P), and Bandcamp.
- yt-dlp-android v2.0.2 for broad multi-site extraction attempts, including Instagram, TikTok, Facebook and X/Twitter where supported.
- Android DownloadManager for progressive streams, saved under `Movies/VideoDownloader`.
- yt-dlp downloads are first written to app-private temporary storage, then published to public media storage. Android 10+ uses MediaStore so Gallery apps can index the downloaded media.

## Important limitations

- Platform support changes; no extractor guarantees every URL works.
- Some links may require cookies or be unavailable due to platform restrictions. DRM, private-content controls, and login/security restrictions are not bypassed.
- yt-dlp-android bundles CPython and yt-dlp in a large AAR and supports arm64-v8a and x86_64 in this project.
- Download only content you own or are authorized to save.

## Dependencies

- NewPipe Extractor: `com.github.teamnewpipe:NewPipeExtractor:v0.27.1`
- yt-dlp-android: `dev.ffmpegkit-maintained:yt-dlp-android:2.0.2`
- Core library desugaring: `com.android.tools:desugar_jdk_libs_nio:2.1.5`

## Licenses

NewPipe Extractor is GPL-3.0-or-later. The yt-dlp-android wrapper is MIT. Preserve the included license and third-party notices, and comply with all applicable license obligations when distributing the app.
