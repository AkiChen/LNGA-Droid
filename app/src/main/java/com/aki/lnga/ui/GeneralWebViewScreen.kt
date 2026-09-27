@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aki.lnga.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun GeneralWebViewScreen(url: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var title by remember(url) { mutableStateOf("网页") }
    var progress by remember(url) { mutableIntStateOf(0) }
    var loading by remember(url) { mutableStateOf(true) }

    fun navigateBack() {
        val view = webView
        if (view?.canGoBack() == true) view.goBack() else onBack()
    }
    BackHandler(onBack = ::navigateBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = ::navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "网页后退")
                    }
                },
                actions = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.Close, contentDescription = "关闭网页") }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            AndroidView(
                factory = {
                    WebView(context).apply {
                        webView = this
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadsImagesAutomatically = true
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        settings.userAgentString = settings.userAgentString + " LNGA-Android"
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress
                                loading = newProgress < 100
                            }

                            override fun onReceivedTitle(view: WebView?, pageTitle: String?) {
                                pageTitle?.takeIf(String::isNotBlank)?.let { title = it }
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: Bitmap?) {
                                loading = true
                            }

                            override fun onPageFinished(view: WebView?, pageUrl: String?) {
                                loading = false
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?,
                            ) {
                                if (request?.isForMainFrame == true) loading = false
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val target = request?.url ?: return false
                                if (target.scheme == "http" || target.scheme == "https") return false
                                return runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, target))
                                    true
                                }.getOrDefault(true)
                            }
                        }
                        loadUrl(url)
                    }
                },
                update = { view ->
                    if (url.isNotBlank() && view.url != url && !view.canGoBack()) view.loadUrl(url)
                },
                modifier = Modifier.fillMaxSize(),
            )
            if (loading) {
                if (progress in 1..99) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    )
                } else {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.apply { stopLoading(); webChromeClient = null; webViewClient = WebViewClient(); destroy() }
            webView = null
        }
    }
}
