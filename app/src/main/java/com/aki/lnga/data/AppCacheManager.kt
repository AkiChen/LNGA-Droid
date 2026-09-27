package com.aki.lnga.data

import android.content.Context
import android.webkit.WebStorage
import android.webkit.WebView
import coil.imageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class AppCacheSnapshot(
    val imageBytes: Long,
    val appCacheBytes: Long,
) {
    val totalBytes: Long get() = maxOf(imageBytes, appCacheBytes)
}

@OptIn(coil.annotation.ExperimentalCoilApi::class)
internal object AppCacheManager {
    suspend fun snapshot(context: Context): AppCacheSnapshot = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        AppCacheSnapshot(
            imageBytes = appContext.imageLoader.diskCache?.size ?: 0L,
            appCacheBytes = directorySize(appContext.cacheDir),
        )
    }

    suspend fun clearMediaAndWebCache(context: Context): AppCacheSnapshot = withContext(Dispatchers.Main) {
        val appContext = context.applicationContext
        appContext.imageLoader.memoryCache?.clear()
        withContext(Dispatchers.IO) {
            appContext.imageLoader.diskCache?.clear()
            appContext.getSharedPreferences("bilibili_preview_cache", Context.MODE_PRIVATE).edit().clear().commit()
            appContext.getSharedPreferences("weibo_preview_cache", Context.MODE_PRIVATE).edit().clear().commit()
            appContext.getSharedPreferences("xiaohongshu_preview_cache", Context.MODE_PRIVATE).edit().clear().commit()
            appContext.getSharedPreferences("douyin_preview_cache", Context.MODE_PRIVATE).edit().clear().commit()
        }
        WebStorage.getInstance().deleteAllData()
        runCatching { WebView(appContext).apply { clearCache(true); destroy() } }
        snapshot(appContext)
    }

    private fun directorySize(file: java.io.File?): Long {
        if (file == null || !file.exists()) return 0L
        if (file.isFile) return file.length()
        return file.listFiles()?.sumOf(::directorySize) ?: 0L
    }
}
