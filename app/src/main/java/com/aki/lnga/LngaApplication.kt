package com.aki.lnga

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.aki.lnga.data.ImageDiagnosticStore
import com.aki.lnga.data.NgaImageProtocolFallbackInterceptor
import com.aki.lnga.data.NgaImageCdnRecoveryInterceptor
import okhttp3.OkHttpClient

class LngaApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        val imageClient = OkHttpClient.Builder()
            .addInterceptor(
                NgaImageCdnRecoveryInterceptor { recovery ->
                    ImageDiagnosticStore.record(
                        this,
                        stage = "cdn-placeholder-retry",
                        purpose = "coil",
                        url = recovery.originalUrl,
                        detail = "placeholder=gif43 cache=${recovery.cacheStatus} age=${recovery.age} " +
                            "requestId=${recovery.requestId} cacheBypass=true",
                    )
                },
            )
            .addInterceptor(
                NgaImageProtocolFallbackInterceptor { _, httpUrl, error ->
                    ImageDiagnosticStore.record(
                        this,
                        stage = "protocol-fallback",
                        purpose = "coil",
                        url = httpUrl,
                        detail = "httpsFailure=${error.javaClass.simpleName} fallback=http",
                    )
                },
            )
            .addNetworkInterceptor { chain ->
                val request = chain.request()
                runCatching { chain.proceed(request) }
                    .onSuccess { response ->
                        ImageDiagnosticStore.record(
                            this,
                            stage = "network",
                            purpose = "coil",
                            url = request.url.toString(),
                            detail = "code=${response.code} final=${response.request.url.host}${response.request.url.encodedPath} " +
                                "type=${response.header("Content-Type").orEmpty().substringBefore(';')} " +
                                "length=${response.header("Content-Length").orEmpty()} " +
                                "cache=${response.header("eo-cache-status").orEmpty()} age=${response.header("age").orEmpty()} " +
                                "requestId=${response.header("x-request-id").orEmpty()}",
                        )
                    }
                    .onFailure { error ->
                        ImageDiagnosticStore.record(
                            this,
                            stage = "network-error",
                            purpose = "coil",
                            url = request.url.toString(),
                            detail = "type=${error.javaClass.simpleName} message=${error.message.orEmpty()}",
                        )
                    }
                    .getOrThrow()
            }
            .build()
        return ImageLoader.Builder(this)
            .okHttpClient(imageClient)
            .components {
                if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory())
                else add(GifDecoder.Factory())
            }
            .build()
    }
}
