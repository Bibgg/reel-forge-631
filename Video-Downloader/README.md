# ALL DOWNLOADER engine

The project uses a layered architecture with first-party download code first and external extractors as limited adapters.

## Download layers

1. **OwnLinkAnalyzer** — first-party HTTP inspection. It follows ordinary redirects and checks response MIME type and Content-Disposition to recognize direct files.
2. **Android DownloadManager** — handles ordinary direct HTTP(S) file URLs and saves them to public device folders.
3. **Embedded Python worker (yt-dlp-android)** — an in-app Python-backed worker for media extraction/download attempts when a direct file path is not appropriate or does not identify a downloadable media stream. The UI invokes this as **PYTHON WORKER FALLBACK**. It is a library-managed runtime, not an interactive system terminal; no arbitrary shell commands are exposed.
4. **NewPipe adapter** — limited fallback for services supported by NewPipeExtractor. Keep usage narrow; it is not the core engine.

## Python worker behavior

- Uses the embedded yt-dlp Android library and its bundled Python runtime.
- Retries transient network/fragment failures and tries a small set of format fallbacks.
- Writes to app-private temporary storage, then publishes completed media to the public media folder.
- It cannot guarantee every URL. It does not bypass DRM, private-content controls, paywalls, login/security checks, CAPTCHAs, expiring access controls or platform enforcement.
- It is intended for media the user owns or is authorized to save.

## Current implementation notes

- Direct file downloads use Android DownloadManager.
- Media fallback is handled by the embedded Python worker.
- NewPipe remains an optional adapter for its supported sources.
- A failed direct DownloadManager task is not yet automatically detected and re-submitted to Python. That requires monitoring DownloadManager completion/failure and coordinating a retry without creating duplicate downloads; it remains a follow-up implementation task.

## Licenses

- NewPipe Extractor: GPL-3.0-or-later; preserve required license and notices.
- yt-dlp-android wrapper: MIT; preserve its notices.
- OkHttp: Apache-2.0.
