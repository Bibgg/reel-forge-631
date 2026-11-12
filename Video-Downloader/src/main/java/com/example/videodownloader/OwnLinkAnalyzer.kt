package com.example.videodownloader

import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.TimeUnit

data class DirectFileInfo(
    val finalUrl: String,
    val fileName: String,
    val extension: String,
    val mimeType: String
)

/**
 * First-party direct HTTP file detection.
 *
 * Tries HEAD first, then a one-byte ranged GET for servers that reject HEAD.
 * Only ordinary public HTTP(S) responses are inspected; this does not defeat
 * authentication, DRM, paywalls, CAPTCHA or other access controls.
 */
object OwnLinkAnalyzer {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(18, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun inspect(rawUrl: String): DirectFileInfo? {
        val url = rawUrl.trim()
        val head = Request.Builder().url(url)
            .head()
            .header("User-Agent", "ALL-DOWNLOADER/1.0 (Android)")
            .build()

        try {
            client.newCall(head).execute().use { response ->
                if (response.isSuccessful) {
                    buildInfo(
                        response.request.url.toString(),
                        response.header("Content-Type"),
                        response.header("Content-Disposition"),
                        response.header("Content-Length")
                    )?.let { return it }
                }
            }
        } catch (_: Exception) {
            // Some legitimate file hosts reject HEAD; try a bounded range request.
        }

        val rangedGet = Request.Builder().url(url)
            .get()
            .header("Range", "bytes=0-0")
            .header("User-Agent", "ALL-DOWNLOADER/1.0 (Android)")
            .build()

        return try {
            client.newCall(rangedGet).execute().use { response ->
                if (!response.isSuccessful && response.code != 206) return null
                val length = response.header("Content-Range")
                    ?.substringAfterLast('/')
                    ?.takeIf { it != "*" }
                    ?: response.header("Content-Length")
                buildInfo(
                    response.request.url.toString(),
                    response.header("Content-Type"),
                    response.header("Content-Disposition"),
                    length
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun buildInfo(
        finalUrl: String,
        rawMime: String?,
        disposition: String?,
        contentLength: String?
    ): DirectFileInfo? {
        val mime = rawMime?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT)
            ?: "application/octet-stream"
        if (mime in setOf("text/html", "application/xhtml+xml", "application/json")) return null

        val dispositionName = disposition?.let { header ->
            Regex("""filename\*=UTF-8''([^;]+)""", RegexOption.IGNORE_CASE)
                .find(header)?.groupValues?.getOrNull(1)?.let {
                    URLDecoder.decode(it, StandardCharsets.UTF_8.name())
                } ?: Regex("""filename="?([^";]+)"?""", RegexOption.IGNORE_CASE)
                    .find(header)?.groupValues?.getOrNull(1)
        }
        val urlName = finalUrl.substringAfterLast('/').substringBefore('?').substringBefore('#')
        val name = (dispositionName ?: urlName).ifBlank { "download" }
            .filter { it.isLetterOrDigit() || it in "._- ()" }
            .take(150).ifBlank { "download" }
        val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
            .ifBlank { extensionForMime(mime) }
        val knownFileMime = mime.startsWith("video/") || mime.startsWith("audio/") ||
            mime.startsWith("image/") || mime in setOf(
                "application/pdf", "application/zip",
                "application/vnd.android.package-archive",
                "application/octet-stream", "application/x-7z-compressed"
            )
        val hasFileName = name.contains('.') || !dispositionName.isNullOrBlank()
        val hasLength = (contentLength?.toLongOrNull() ?: 0L) > 0L
        if (!knownFileMime && !hasFileName && !hasLength) return null
        return DirectFileInfo(finalUrl, name, ext, mime)
    }

    private fun extensionForMime(mime: String): String = when {
        mime == "application/pdf" -> "pdf"
        mime == "application/zip" -> "zip"
        mime == "application/vnd.android.package-archive" -> "apk"
        mime.startsWith("audio/") -> "audio"
        mime.startsWith("video/") -> "video"
        mime.startsWith("image/") -> "image"
        else -> "bin"
    }
}
