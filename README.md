# ReelForge — Android video app

Kotlin और Jetpack Compose पर बना Android ऐप।

## सुविधाएँ

- गैलरी से वीडियो चुनकर ऐप में सेव करना
- सेव किए वीडियो की ग्रिड और फ़ाइल सूची
- फ़ुलस्क्रीन Media3 वीडियो प्लेयर
- Video Downloader टैब: NewPipe Extractor और yt-dlp-android के जरिए वीडियो लिंक जाँचना और डाउनलोड करना
- Android 10+ पर डाउनलोड सार्वजनिक Downloads/ReelForge फ़ोल्डर में सेव होते हैं

## Platform support

- NewPipe Extractor: YouTube, SoundCloud, media.ccc.de, PeerTube (no P2P), Bandcamp.
- yt-dlp-android: 1000+ extractors का दावा; Instagram, TikTok, Facebook और X/Twitter सहित कई साइटों पर डाउनलोड की कोशिश।
- हर URL के काम करने की गारंटी नहीं है। कुछ साइटों पर cookies या login की जरूरत हो सकती है; free yt-dlp-android build में curl-cffi TLS impersonation नहीं है।
- DRM, निजी सामग्री या login restrictions bypass नहीं किए जाते। केवल अधिकृत वीडियो डाउनलोड करें।

## Downloader module

पूरा module, setup और third-party licenses: Video-Downloader/

ध्यान दें: NewPipe Extractor GPL-3.0-or-later है। Combined app distribute करने पर GPL की source-sharing और license obligations लागू हो सकती हैं। yt-dlp-android wrapper MIT है; उसके bundled components के notices module में शामिल हैं।

## तकनीकी जानकारी

- Package: com.example.reelforge
- minSdk 24, targetSdk 34
- AGP 8.5.2, Kotlin 2.0.20, Java 17
- Native ABIs: arm64-v8a और x86_64
- yt-dlp-android AAR लगभग 60–80 MB अतिरिक्त आकार जोड़ सकता है

## APK build

GitHub पर Actions टैब खोलें और Build Android APK and AAB workflow चुनें। सफल build के बाद ReelForge-APK-and-AAB artifact डाउनलोड करें।
