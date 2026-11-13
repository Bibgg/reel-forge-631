package com.example.videodownloader

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class YoutubeSearchItem(
    val title: String,
    val url: String,
    val thumbnailUrl: String,
    val uploader: String,
    val durationSeconds: Long,
    val views: String
)

/**
 * App integration layer around NewPipe Extractor.
 * Search results are converted into app-owned models; UI remains ALL DOWNLOADER's.
 */
object YoutubeSearchEngine {
    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    @Volatile private var initialized = false

    @Synchronized
    private fun ensureInitialized() {
        if (initialized) return
        NewPipe.init(object : Downloader() {
            override fun execute(request: Request): Response {
                val builder = okhttp3.Request.Builder().url(request.url())
                request.headers().forEach { (name, values) ->
                    values.forEach { value -> builder.addHeader(name, value) }
                }
                val body = request.dataToSend()
                val httpRequest = when (request.httpMethod()) {
                    "POST" -> builder.post((body ?: ByteArray(0)).toRequestBody()).build()
                    "PUT" -> builder.put((body ?: ByteArray(0)).toRequestBody()).build()
                    "HEAD" -> builder.head().build()
                    else -> builder.get().build()
                }
                client.newCall(httpRequest).execute().use { response ->
                    val responseHeaders = response.headers.toMultimap()
                    return Response(
                        response.code,
                        response.message,
                        responseHeaders,
                        response.body?.string().orEmpty(),
                        response.request.url.toString()
                    )
                }
            }
        })
        initialized = true
    }

    suspend fun searchVideos(query: String, limit: Int = 30): List<YoutubeSearchItem> =
        withContext(Dispatchers.IO) {
            require(query.isNotBlank()) { "वीडियो खोजने के लिए नाम लिखें।" }
            ensureInitialized()
            val youtube = NewPipe.getServiceByName("YouTube")
            val extractor = youtube.getSearchExtractor(query, listOf("videos"), "")
            extractor.fetchPage()
            extractor.initialPage.items
                .filterIsInstance<StreamInfoItem>()
                .take(limit)
                .map { item ->
                    YoutubeSearchItem(
                        title = item.name,
                        url = item.url,
                        thumbnailUrl = item.thumbnails.firstOrNull()?.url.orEmpty(),
                        uploader = item.uploaderName,
                        durationSeconds = item.duration,
                        views = item.viewCount?.toString().orEmpty()
                    )
                }
        }
}
