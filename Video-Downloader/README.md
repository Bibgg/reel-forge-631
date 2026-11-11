# ReelForge Video Downloader

This Android library module is wired into ReelForge. It combines two extraction engines with Android download handling:

- NewPipe Extractor v0.27.1 for YouTube, SoundCloud, media.ccc.de, PeerTube (no P2P), and Bandcamp.
- yt-dlp-android v2.0.2 for broad multi-site downloads (yt-dlp advertises 1000+ extractors), including attempts for Instagram, TikTok, Facebook and X/Twitter.
- Android DownloadManager for NewPipe progressive streams.
- yt-dlp downloads are written to app storage first, then copied into public Downloads/ReelForge on Android 10+.

## Important limitations

- Support varies by platform and changes over time. A listed extractor does not guarantee every URL works.
- Some Instagram, TikTok, Facebook or X links may require cookies or be blocked by platform-side restrictions. The free yt-dlp-android build does not include curl-cffi TLS fingerprint impersonation. The app does not bypass DRM, private-content controls, or login restrictions.
- yt-dlp-android bundles CPython 3.13 and yt-dlp in an AAR of approximately 60–80 MB. It supports arm64-v8a and x86_64 only; the app's native ABI filters are set accordingly.
- Downloads should be limited to videos owned by the user or content the user is authorized to save. Respect platform terms and copyright law.

## Gradle dependencies

- NewPipe Extractor: com.github.teamnewpipe:NewPipeExtractor:v0.27.1
- yt-dlp-android: dev.ffmpegkit-maintained:yt-dlp-android:2.0.2
- Core library desugaring: com.android.tools:desugar_jdk_libs_nio:2.1.5

## License notices

NewPipe Extractor is GPL-3.0-or-later. Its full license is in LICENSE-NewPipeExtractor. The yt-dlp-android wrapper is MIT; see LICENSE-yt-dlp-android and THIRD-PARTY-NOTICES-yt-dlp.txt for bundled third-party notices. Distribution of the combined app must comply with the NewPipe GPL obligations and preserve all third-party notices.
