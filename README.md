# ReelForge — Android video app

Kotlin और Jetpack Compose पर बना Android ऐप।

## सुविधाएँ

- गैलरी से वीडियो चुनकर ऐप में सेव करना
- सेव किए वीडियो की ग्रिड और फ़ाइल सूची
- फ़ुलस्क्रीन Media3 वीडियो प्लेयर
- Video Downloader टैब: NewPipe Extractor से upstream-supported सेवाओं की स्ट्रीम जाँच और Android DownloadManager से डाउनलोड
- सीधे MP4/WebM जैसे अधिकृत media-file links डाउनलोड करना

## Downloader support की स्थिति

NewPipe Extractor upstream: YouTube, SoundCloud, media.ccc.de, PeerTube (no P2P), Bandcamp.

Instagram, TikTok, Facebook और X/Twitter लिंक पहचाने जाते हैं, लेकिन NewPipe Extractor इन्हें सपोर्ट नहीं करता। इनके लिए अलग अधिकृत provider/API integration आवश्यक है। ऐप निजी सामग्री या platform restrictions bypass नहीं करता।

Downloader module का कोड, setup और third-party license notice `Video-Downloader/` में है। NewPipe Extractor GPL-3.0-or-later है; वितरण से पहले GPL की शर्तें पूरी करें।

## तकनीकी जानकारी

- Package: `com.example.reelforge`
- minSdk 24, targetSdk 34
- AGP 8.5.2, Kotlin 2.0.20, Java 17
- Gradle 8.9 in GitHub Actions

## APK build

GitHub पर Actions टैब खोलें और Build Android APK and AAB workflow चुनें। सफल build के बाद `ReelForge-APK-and-AAB` artifact डाउनलोड करें।