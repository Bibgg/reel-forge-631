# ALL DOWNLOADER engine

The project is moving toward a first-party, layered download architecture. NewPipeExtractor is a limited optional adapter, not the core engine.

## Engine layers

1. **OwnLinkAnalyzer** — first-party HTTP inspection. It follows ordinary redirects and checks response MIME type and Content-Disposition to recognize direct files even when the URL has no obvious extension.
2. **Own HTTP transfer layer (next milestone)** — planned resumable/range-aware transfer, checksum validation, safe filename handling and retry policy for ordinary public file URLs.
3. **yt-dlp adapter** — broad media-site extraction where the upstream extractor supports the source. Support is not guaranteed and changes over time.
4. **NewPipe adapter** — limited fallback for services supported by NewPipeExtractor. Keep usage narrow; do not make it the primary architecture.
5. **Android storage adapter** — DownloadManager/MediaStore for public device storage.

## Current behavior

- Direct media/file URLs can be downloaded with Android DownloadManager.
- Media extraction currently uses the yt-dlp adapter; the NewPipe-based inspection path remains available for supported progressive streams.
- OwnLinkAnalyzer is integrated as the first inspection step for direct HTTP files.
- Planned improvements are not claimed as implemented until committed, built and tested.

## Safety and limitations

- No extractor can guarantee every site or every URL.
- The app does not bypass DRM, private-content controls, paywalls, login/security checks, CAPTCHAs, expiring access controls or platform enforcement.
- Download only files and media that you own or are authorized to save.
- Do not use the app to retrieve malware, stolen data or illicit material.

## Dependencies and licenses

- NewPipe Extractor: GPL-3.0-or-later; included license and notices must be preserved.
- yt-dlp-android: MIT wrapper; included notices must be preserved.
- OkHttp: Apache-2.0.
