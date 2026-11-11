# ALL DOWNLOADER — Android

A single-link Android download manager for direct files and supported online media.

## What it can download
- Direct HTTP(S) files: APK packages, PDF, office documents, archives, images, audio, video and other ordinary files.
- Supported media pages through yt-dlp and NewPipe Extractor, where the source and platform permit extraction.
- Google Play app links open in the official Play Store. They are not converted into APK downloads.

## Save locations
- Videos: `Movies/AllDownloader`
- Audio: `Music/AllDownloader`
- APK, PDF, ZIP, images and other files: `Download/AllDownloader`

## Limits
No downloader can fetch every URL. Direct file URLs are required for arbitrary file types; websites with DRM, authentication, private content, expiring links or anti-bot restrictions may not work. The app does not bypass those protections. Download only content you own or are authorized to save.

## Build
Use the **ALL DOWNLOADER Android Build** GitHub Actions workflow. The APK is uploaded as the `ALL-DOWNLOADER-debug-apk` artifact.
