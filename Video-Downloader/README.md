# ReelForge Video Downloader

This Android library module is wired into ReelForge and uses NewPipe Extractor v0.27.1.

## Current extraction support

NewPipe Extractor upstream currently lists YouTube, SoundCloud, media.ccc.de, PeerTube (no P2P), and Bandcamp.

The app recognizes Instagram, TikTok, Facebook and X/Twitter links, but the upstream extractor does not currently support those services. They are reported as unsupported instead of pretending to download them. A future integration must use an authorized provider/API and respect each service's terms, privacy controls and copyright rules.

Direct media-file URLs (for example, an authorized MP4 URL) can be sent to Android DownloadManager.

## Android integration

- Module: :Video-Downloader
- NewPipe Extractor: com.github.teamnewpipe:NewPipeExtractor:v0.27.1
- Downloads are handled by Android DownloadManager and saved under the device's public Downloads folder.
- Network extraction runs on Dispatchers.IO.
- NewPipe Extractor requires Java 17 and core-library desugaring with desugar_jdk_libs_nio for minSdk below 33.

## License notice

NewPipe Extractor is licensed under GNU GPL v3 or later. See THIRD_PARTY_NOTICES.md and LICENSE-NewPipeExtractor. If distributing a combined app, comply with GPL obligations, including providing corresponding source and license notices.
