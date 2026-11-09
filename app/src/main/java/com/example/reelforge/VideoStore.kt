package com.example.reelforge

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File

/** ऐप के अपने स्टोरेज में वीडियो सेव करता है और उनकी लिस्ट व थंबनेल देता है। */
object VideoStore {

    private fun dir(context: Context): File =
        File(context.filesDir, "videos").apply { mkdirs() }

    fun list(context: Context): List<File> =
        dir(context).listFiles()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    fun save(context: Context, uri: Uri): File? {
        return try {
            val target = File(dir(context), "video_${System.currentTimeMillis()}.mp4")
            val input = context.contentResolver.openInputStream(uri) ?: return null
            input.use { source ->
                target.outputStream().use { sink -> source.copyTo(sink) }
            }
            target
        } catch (e: Exception) {
            null
        }
    }

    fun thumbnail(file: File): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.getFrameAtTime(1_000_000L) ?: retriever.getFrameAtTime(0L)
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }
}
