# Bypass & Safe Fallbacks

This folder is the home for resilient, permission-respecting download fallback logic in ALL DOWNLOADER.

## Purpose

When the primary download route fails, the app may try another supported route instead of immediately giving up.

## Planned strategies

1. **Network retry** — bounded retries for temporary connection errors, timeouts and interrupted transfers.
2. **Extractor fallback** — try another maintained extractor only when it supports the same public/authorized resource.
3. **Direct URL fallback** — use a direct HTTP(S) file URL when the source provides one.
4. **Resume** — continue a partial transfer when the server supports byte ranges and the URL remains valid.
5. **Authorized mirrors** — allow a mirror only when it is supplied by the content owner or otherwise authorized.
6. **Clear failure diagnosis** — distinguish unsupported source, expired link, missing permission, unavailable server, DRM, and network errors.

## Safety boundary

This module must not defeat authentication, paywalls, DRM, private-content controls, signed URL expiry, rate limits, CAPTCHAs, or other access/security restrictions. It must not steal cookies, credentials, tokens, or private session data. It must not disguise abusive traffic or evade platform enforcement.

If access is denied, the app should explain the reason and direct the user to the official authorized access route.

## Implementation rules

- Use HTTPS by default; allow HTTP only when the user explicitly accepts an insecure source.
- Set connection and read timeouts.
- Limit retries with exponential backoff and jitter; do not retry permanent 4xx/access-denied failures endlessly.
- Verify final file size and, when provided by the source, checksum.
- Sanitize filenames and prevent path traversal.
- Keep downloads in Android-managed public media/download locations.
- Never report success until the file is fully written and verified.

This directory is a design scaffold. Runtime fallback strategies must be implemented and tested before being advertised as available.
