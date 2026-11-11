# Video Downloader — Android

यह प्रोजेक्ट अब केवल Android वीडियो/ऑडियो डाउनलोडर है। पुराने वीडियो अपलोड, लोकल वीडियो लाइब्रेरी, फ़ाइल ब्राउज़र और इन-ऐप वीडियो प्लेयर फीचर हटा दिए गए हैं।

## मुख्य सुविधाएँ
- वीडियो लिंक जाँचने और उपलब्ध डाउनलोड विकल्प दिखाने की व्यवस्था
- NewPipe Extractor और yt-dlp-android आधारित extraction/download engines
- Android DownloadManager और MediaStore के जरिए सार्वजनिक मोबाइल स्टोरेज में सेव करना
- वीडियो: `Movies/VideoDownloader`
- ऑडियो: `Music/VideoDownloader`
- Android Gallery/Media ऐप्स के लिए MediaStore indexing (Android 10+)

## Platform support
- NewPipe Extractor: YouTube, SoundCloud, media.ccc.de, PeerTube और Bandcamp के समर्थित streams.
- yt-dlp-android: अनेक साइटों के लिए extraction की कोशिश; हर URL या हर platform की सफलता की गारंटी नहीं।
- Instagram, TikTok, Facebook और X/Twitter के लिए platform restrictions, login/cookies या बदलते APIs के कारण कुछ लिंक काम नहीं कर सकते।
- DRM, निजी सामग्री या login/security restrictions bypass नहीं किए जाते। केवल वही सामग्री डाउनलोड करें जिसे सेव करने की अनुमति है।

## Third-party licenses
Downloader module और third-party notices: `Video-Downloader/`.
NewPipe Extractor GPL-3.0-or-later है; app को distribute करने से पहले GPL obligations और bundled dependency notices का पालन करें।

## Build
GitHub Actions में **Android APK Build** workflow चलाएँ। सफल build के बाद `ReelForge-debug-apk` artifact से APK लें।
