package com.aki.lnga.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.SystemClock
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.util.concurrent.atomic.AtomicBoolean
import com.aki.lnga.BuildConfig
import org.json.JSONObject

private const val LOGIN_URL = "https://bbs.nga.cn/nuke.php?__lib=login&__act=account&login"
private val NGA_HOSTS = setOf("bbs.nga.cn", "ngabbs.com", "nga.178.com")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WebLoginScreen(
    verifying: Boolean,
    resetSessionBeforeLoad: Boolean,
    onSessionReset: () -> Unit,
    onCredentials: (uid: String, cid: String) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val latestOnCredentials by rememberUpdatedState(onCredentials)
    val captured = remember { AtomicBoolean(false) }
    val webViewHolder = remember { arrayOfNulls<WebView>(1) }
    var pageLoading by remember { mutableStateOf(true) }
    var pageProblem by remember { mutableStateOf<String?>(null) }
    var pageStage by remember { mutableStateOf("准备加载") }
    var showDiagnostics by remember { mutableStateOf(false) }
    val partialSessionBeforeLoad = remember { hasPartialNgaPassportCookies() }
    val effectiveSessionReset = resetSessionBeforeLoad || partialSessionBeforeLoad
    var webSessionReady by remember(effectiveSessionReset) { mutableStateOf(!effectiveSessionReset) }
    val latestWebSessionReady by rememberUpdatedState(webSessionReady)
    val diagnosticEvents = remember { mutableStateListOf<String>() }
    val diagnosticStartedAt = remember { SystemClock.elapsedRealtime() }
    BackHandler(onBack = onCancel)

    fun diagnostic(message: String) {
        val line = "+${SystemClock.elapsedRealtime() - diagnosticStartedAt}ms $message"
        if (diagnosticEvents.size >= 80) diagnosticEvents.removeAt(0)
        diagnosticEvents += line
        if (BuildConfig.DEBUG) Log.d("LNGA.WebLogin", line)
    }

    LaunchedEffect(Unit) {
        diagnostic(webLoginEnvironment(context))
        diagnostic("cookie-presence ${ngaCookiePresence()}")
        diagnostic("initial-reset-required=$resetSessionBeforeLoad partial-cookie-reset=$partialSessionBeforeLoad")
    }

    fun submit(uid: String, cid: String) {
        if (uid.isNotBlank() && cid.isNotBlank() && captured.compareAndSet(false, true)) {
            latestOnCredentials(uid, cid)
        }
    }

    fun inspectCookies(url: String?) {
        val manager = CookieManager.getInstance()
        val candidates = listOfNotNull(url, "https://bbs.nga.cn", "https://ngabbs.com", "https://nga.178.com")
        candidates.forEach { candidate ->
            val credentials = parseNgaCookies(manager.getCookie(candidate))
            if (credentials != null) {
                diagnostic("credentials detected host=${safeWebUrl(candidate)} values=redacted")
                submit(credentials.first, credentials.second)
                return@forEach
            }
        }
    }

    fun cleanAndReload() {
        val webView = webViewHolder[0]
        if (webView == null) {
            diagnostic("clean-reload skipped: WebView unavailable")
            return
        }
        pageProblem = null
        pageLoading = true
        pageStage = "正在清理网页状态"
        webSessionReady = false
        captured.set(false)
        diagnostic("manual clean-reload started; before=${ngaCookiePresence()}")
        clearNgaWebSession {
            diagnostic("manual clean completed; after=${ngaCookiePresence()}")
            webSessionReady = true
            webView.post {
                webView.clearCache(true)
                webView.clearHistory()
                pageStage = "重新加载"
                diagnostic("loadUrl after manual clean ${safeWebUrl(LOGIN_URL)}")
                webView.loadUrl(LOGIN_URL)
            }
        }
    }

    DisposableEffect(Unit) {
        val handler = Handler(Looper.getMainLooper())
        val poll = object : Runnable {
            override fun run() {
                if (!captured.get() && latestWebSessionReady) {
                    inspectCookies(webViewHolder[0]?.url)
                    handler.postDelayed(this, 1_000)
                }
            }
        }
        handler.post(poll)
        onDispose {
            handler.removeCallbacks(poll)
            webViewHolder[0]?.apply {
                stopLoading()
                webChromeClient = null
                webViewClient = WebViewClient()
                destroy()
            }
            webViewHolder[0] = null
        }
    }

    Scaffold(
        modifier = Modifier.edgeSwipeBack(onCancel),
        topBar = {
            TopAppBar(
                title = { Text("NGA 网页登录") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "取消登录")
                    }
                },
                actions = {
                    TextButton(onClick = { showDiagnostics = true }) {
                        Icon(Icons.Default.BugReport, contentDescription = null)
                        Text("诊断 ${diagnosticEvents.size}")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            LoginWebView(
                modifier = Modifier.fillMaxSize(),
                resetSessionBeforeLoad = effectiveSessionReset,
                onSessionReset = {
                    webSessionReady = true
                    diagnostic("initial web session is ready")
                    onSessionReset()
                },
                onCreated = {
                    webViewHolder[0] = it
                    it.post { diagnostic("WebView created") }
                },
                onPageLoading = {
                    pageLoading = it
                    pageStage = if (it) "页面加载中" else "页面加载结束"
                    if (it) pageProblem = null
                },
                onPageFinished = { url -> inspectCookies(url) },
                onConsoleCredentials = ::submit,
                onProblem = {
                    pageProblem = it
                    pageStage = "检测到异常"
                    diagnostic("problem: $it")
                },
                onDiagnostic = ::diagnostic,
            )
            if (pageLoading || verifying) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = if (verifying) "正在验证登录信息…" else "正在加载登录页面…",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            pageProblem?.let { problem ->
                Card(
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("登录页没有正常显示", style = MaterialTheme.typography.titleMedium)
                        Text(problem, style = MaterialTheme.typography.bodyMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                modifier = Modifier.weight(1f),
                                onClick = ::cleanAndReload,
                            ) { Text("清理并重试") }
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    pageProblem = null
                                    pageLoading = true
                                    diagnostic("plain reload requested")
                                    webViewHolder[0]?.reload()
                                },
                            ) { Text("普通重载") }
                        }
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                diagnostic("external browser diagnostic requested")
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LOGIN_URL)))
                                }
                            },
                        ) { Text("浏览器诊断") }
                        Text(
                            "浏览器入口只用于确认网页能否打开；浏览器 Cookie 无法自动回传给 App。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (showDiagnostics) {
                WebLoginDiagnosticDialog(
                    stage = pageStage,
                    events = diagnosticEvents,
                    onCleanRetry = {
                        showDiagnostics = false
                        cleanAndReload()
                    },
                    onDismiss = { showDiagnostics = false },
                    onCopy = { report ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("LNGA 网页登录诊断", report))
                        Toast.makeText(context, "诊断信息已复制（不含登录凭据）", Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun LoginWebView(
    modifier: Modifier,
    resetSessionBeforeLoad: Boolean,
    onSessionReset: () -> Unit,
    onCreated: (WebView) -> Unit,
    onPageLoading: (Boolean) -> Unit,
    onPageFinished: (String?) -> Unit,
    onConsoleCredentials: (String, String) -> Unit,
    onProblem: (String) -> Unit,
    onDiagnostic: (String) -> Unit,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            CookieManager.getInstance().setAcceptCookie(true)
            val partialPassportCookies = hasPartialNgaPassportCookies()
            var promotedLoginFrame = false
            WebView(context).apply {
                onCreated(this)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                settings.safeBrowsingEnabled = true
                settings.userAgentString = "${settings.userAgentString} LNGA-Android/${BuildConfig.VERSION_NAME}"
                settings.useWideViewPort = false
                settings.loadWithOverviewMode = false
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        onDiagnostic("page-start main=${safeWebUrl(url)}")
                        onPageLoading(true)
                    }

                    override fun onPageCommitVisible(view: WebView?, url: String?) {
                        onDiagnostic("page-visible main=${safeWebUrl(url)} contentHeight=${view?.contentHeight}")
                        view?.let { webView ->
                            repairOrPromoteLoginFrame(webView, "page-visible", onDiagnostic) { frameUrl ->
                                if (!promotedLoginFrame) {
                                    promotedLoginFrame = true
                                    onDiagnostic("promoting zero-height login iframe to main page ${safeWebUrl(frameUrl)}")
                                    webView.loadUrl(frameUrl, mapOf("Referer" to (webView.url ?: LOGIN_URL)))
                                }
                            }
                        }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        onPageLoading(false)
                        onDiagnostic(
                            "page-finished main=${safeWebUrl(url)} titleLength=${view?.title?.length ?: 0} " +
                                "progress=${view?.progress} contentHeight=${view?.contentHeight} cookies=${ngaCookiePresence()}",
                        )
                        onPageFinished(url)
                        view?.let { webView ->
                            webView.postDelayed({
                                repairOrPromoteLoginFrame(webView, "page-finished+250ms", onDiagnostic) { frameUrl ->
                                    if (!promotedLoginFrame) {
                                        promotedLoginFrame = true
                                        onDiagnostic("promoting zero-height login iframe to main page ${safeWebUrl(frameUrl)}")
                                        webView.loadUrl(frameUrl, mapOf("Referer" to (webView.url ?: LOGIN_URL)))
                                    }
                                }
                            }, 250)
                            webView.postDelayed({ inspectLoginDocument(webView, "audit-1.5s", onDiagnostic, onProblem) }, 1_500)
                            webView.postDelayed({ inspectLoginDocument(webView, "audit-6s", onDiagnostic, onProblem) }, 6_000)
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?,
                    ) {
                        super.onReceivedError(view, request, error)
                        onDiagnostic(
                            "resource-error main=${request?.isForMainFrame} code=${error?.errorCode} " +
                                "description=${error?.description} url=${safeWebUrl(request?.url?.toString())}",
                        )
                        if (request?.isForMainFrame == true || isNgaLoginFrameUrl(request?.url?.toString())) {
                            onPageLoading(false)
                            onProblem("登录页面加载失败（WebView ${error?.errorCode ?: "未知错误"}），请检查网络后重试。")
                        }
                    }

                    override fun onReceivedHttpError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        errorResponse: WebResourceResponse?,
                    ) {
                        super.onReceivedHttpError(view, request, errorResponse)
                        onDiagnostic(
                            "http-error main=${request?.isForMainFrame} status=${errorResponse?.statusCode} " +
                                "reason=${errorResponse?.reasonPhrase} url=${safeWebUrl(request?.url?.toString())}",
                        )
                        if (request?.isForMainFrame == true || isNgaLoginFrameUrl(request?.url?.toString())) {
                            onPageLoading(false)
                            onProblem("NGA 登录页面返回 HTTP ${errorResponse?.statusCode ?: "错误"}，请稍后重试。")
                        }
                    }

                    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                        handler?.cancel()
                        onDiagnostic("ssl-error primary=${error?.primaryError} url=${safeWebUrl(error?.url)}")
                        onPageLoading(false)
                        onProblem("NGA 登录页发生 SSL 证书错误（${error?.primaryError ?: "未知"}），已阻止继续加载。")
                    }

                    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                        onDiagnostic("renderer-gone crashed=${detail?.didCrash()} priority=${detail?.rendererPriorityAtExit()}")
                        onPageLoading(false)
                        onProblem("Android System WebView 渲染进程已停止，请更新 WebView/Chrome 后重试。")
                        view?.destroy()
                        return true
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val uri = request?.url ?: return false
                        onDiagnostic("navigation main=${request.isForMainFrame} method=${request.method} url=${safeWebUrl(uri.toString())}")
                        if (uri.scheme == "https" && uri.host in NGA_HOSTS) return false
                        if (request.isForMainFrame) {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                            return true
                        }
                        return false
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    private var lastProgressBucket = -1

                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        val bucket = newProgress / 20
                        if (bucket != lastProgressBucket || newProgress == 100) {
                            lastProgressBucket = bucket
                            onDiagnostic("progress=$newProgress url=${safeWebUrl(view?.url)}")
                        }
                    }

                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        val message = consoleMessage?.message().orEmpty()
                        onDiagnostic(
                            "console level=${consoleMessage?.messageLevel()} line=${consoleMessage?.lineNumber()} " +
                                "source=${safeWebUrl(consoleMessage?.sourceId())} messageLength=${message.length} " +
                                "messageHash=${message.hashCode()} loginSignal=${message.contains("loginSuccess")}",
                        )
                        if (message.contains("loginSuccess")) {
                            runCatching {
                                val json = JSONObject(message.substringAfter(':').trim())
                                val uid = json.optString("uid")
                                val cid = json.optString("token")
                                if (uid.isNotBlank() && cid.isNotBlank()) onConsoleCredentials(uid, cid)
                            }
                        }
                        return true
                    }
                }
                post {
                    if (resetSessionBeforeLoad || partialPassportCookies) {
                        onDiagnostic(
                            "initial NGA web-session clear started; requested=$resetSessionBeforeLoad " +
                                "partialCookies=$partialPassportCookies before=${ngaCookiePresence()}",
                        )
                        clearNgaWebSession {
                            post {
                                onDiagnostic("initial NGA web-session clear completed; after=${ngaCookiePresence()}")
                                onSessionReset()
                                clearCache(true)
                                clearHistory()
                                onDiagnostic("loadUrl after initial clear ${safeWebUrl(LOGIN_URL)}")
                                loadUrl(LOGIN_URL)
                            }
                        }
                    } else {
                        onDiagnostic("loadUrl ${safeWebUrl(LOGIN_URL)}")
                        loadUrl(LOGIN_URL)
                    }
                }
            }
        },
    )
}

private fun repairOrPromoteLoginFrame(
    webView: WebView,
    label: String,
    onDiagnostic: (String) -> Unit,
    onPromote: (String) -> Unit,
) {
    val script = """
        (function() {
          var frame = document.getElementById('iff');
          if (!frame) return JSON.stringify({frameFound:false});
          var before = frame.getBoundingClientRect();
          var targetHeight = Math.max(window.innerHeight, 480);
          frame.setAttribute('height', String(targetHeight));
          frame.style.setProperty('display', 'block', 'important');
          frame.style.setProperty('width', '100%', 'important');
          frame.style.setProperty('height', targetHeight + 'px', 'important');
          frame.style.setProperty('min-height', targetHeight + 'px', 'important');
          var after = frame.getBoundingClientRect();
          return JSON.stringify({
            frameFound: true,
            src: frame.src || '',
            beforeWidth: Math.round(before.width),
            beforeHeight: Math.round(before.height),
            afterWidth: Math.round(after.width),
            afterHeight: Math.round(after.height),
            computedHeight: getComputedStyle(frame).height,
            viewportWidth: window.innerWidth,
            viewportHeight: window.innerHeight
          });
        })();
    """.trimIndent()
    webView.evaluateJavascript(script) { rawResult ->
        val result = runCatching { JSONObject(decodeJavascriptString(rawResult)) }.getOrNull()
        val frameFound = result?.optBoolean("frameFound") == true
        val frameUrl = result?.optString("src").orEmpty()
        val beforeWidth = result?.optInt("beforeWidth", -1) ?: -1
        val beforeHeight = result?.optInt("beforeHeight", -1) ?: -1
        val afterWidth = result?.optInt("afterWidth", -1) ?: -1
        val afterHeight = result?.optInt("afterHeight", -1) ?: -1
        onDiagnostic(
            "$label frame-repair frame=$frameFound before=${beforeWidth}x$beforeHeight " +
                "after=${afterWidth}x$afterHeight computedHeight=${result?.optString("computedHeight")} " +
                "viewport=${result?.optInt("viewportWidth")}x${result?.optInt("viewportHeight")} " +
                "frameUrl=${safeWebUrl(frameUrl)}",
        )
        if (frameFound && afterHeight <= 1 && isNgaLoginFrameUrl(frameUrl)) onPromote(frameUrl)
    }
}

private fun inspectLoginDocument(
    webView: WebView,
    label: String,
    onDiagnostic: (String) -> Unit,
    onProblem: (String) -> Unit,
) {
    val script = """
        (function() {
          var frame = document.getElementById('iff');
          var body = document.body;
          var result = {
            readyState: document.readyState || 'unknown',
            bodyTextLength: body ? (body.innerText || '').trim().length : -1,
            bodyChildren: body ? body.children.length : -1,
            bodyHeight: body ? Math.round(body.getBoundingClientRect().height) : -1,
            documentHeight: body ? body.scrollHeight : -1,
            viewportWidth: window.innerWidth,
            viewportHeight: window.innerHeight,
            documentClientWidth: document.documentElement ? document.documentElement.clientWidth : -1,
            documentClientHeight: document.documentElement ? document.documentElement.clientHeight : -1,
            iframeStatus: frame ? 'present' : 'missing',
            iframeSrc: frame ? (frame.src || '') : '',
            iframeWidth: frame ? Math.round(frame.getBoundingClientRect().width) : -1,
            iframeHeight: frame ? Math.round(frame.getBoundingClientRect().height) : -1,
            iframeTextLength: -1,
            iframeChildren: -1,
            iframeComputedHeight: frame ? getComputedStyle(frame).height : '',
            iframeDisplay: frame ? getComputedStyle(frame).display : '',
            iframeVisibility: frame ? getComputedStyle(frame).visibility : '',
            iframePosition: frame ? getComputedStyle(frame).position : ''
          };
          if (frame) {
            try {
              var doc = frame.contentDocument;
              if (!doc || !doc.body) {
                result.iframeStatus = 'empty-document';
              } else {
                result.iframeTextLength = (doc.body.innerText || '').trim().length;
                result.iframeChildren = doc.body.children.length;
                result.iframeStatus = (result.iframeTextLength > 0 || result.iframeChildren > 0) ? 'ready' : 'empty';
              }
            } catch (e) {
              result.iframeStatus = 'inaccessible';
            }
          }
          return JSON.stringify(result);
        })();
    """.trimIndent()
    webView.evaluateJavascript(script) { rawResult ->
        val decoded = decodeJavascriptString(rawResult)
        val result = runCatching { JSONObject(decoded) }.getOrNull()
        if (result == null) {
            onDiagnostic("$label DOM audit parse-failed rawLength=${rawResult.length}")
            return@evaluateJavascript
        }
        val ready = result.optString("readyState", "unknown")
        val textLength = result.optInt("bodyTextLength", -1)
        val children = result.optInt("bodyChildren", -1)
        val bodyHeight = result.optInt("bodyHeight", -1)
        val documentHeight = result.optInt("documentHeight", -1)
        val viewportWidth = result.optInt("viewportWidth", -1)
        val viewportHeight = result.optInt("viewportHeight", -1)
        val clientWidth = result.optInt("documentClientWidth", -1)
        val clientHeight = result.optInt("documentClientHeight", -1)
        val frameStatus = result.optString("iframeStatus", "unknown")
        val frameWidth = result.optInt("iframeWidth", -1)
        val frameHeight = result.optInt("iframeHeight", -1)
        val frameText = result.optInt("iframeTextLength", -1)
        val frameChildren = result.optInt("iframeChildren", -1)
        val frameUrl = safeWebUrl(result.optString("iframeSrc"))
        val computedHeight = result.optString("iframeComputedHeight", "unknown")
        val display = result.optString("iframeDisplay", "unknown")
        val visibility = result.optString("iframeVisibility", "unknown")
        val position = result.optString("iframePosition", "unknown")
        onDiagnostic(
            "$label DOM ready=$ready bodyText=$textLength bodyChildren=$children bodyHeight=$bodyHeight " +
                "documentHeight=$documentHeight viewport=${viewportWidth}x$viewportHeight client=${clientWidth}x$clientHeight " +
                "iframe=$frameStatus iframeSize=${frameWidth}x$frameHeight computedHeight=$computedHeight " +
                "display=$display visibility=$visibility position=$position iframeText=$frameText " +
                "iframeChildren=$frameChildren iframeUrl=$frameUrl",
        )
        if (label == "audit-1.5s" && frameStatus != "missing" && (frameWidth <= 1 || frameHeight <= 1)) {
            onDiagnostic("$label detected zero-sized iframe after repair attempt")
        }
        if (label == "audit-6s") {
            when {
                ready == "complete" && textLength <= 0 && children <= 0 ->
                    onProblem("NGA 页面已完成加载，但主文档没有任何可见内容。请复制诊断信息后尝试清理并重试。")
                frameStatus == "empty" || frameStatus == "empty-document" ->
                    onProblem("NGA 外层页面已加载，但登录 iframe 仍为空白。请复制诊断信息后尝试清理并重试。")
                frameStatus == "missing" && textLength in 0..9 ->
                    onProblem("NGA 页面没有创建登录 iframe，也没有足够的正文内容。请复制诊断信息后尝试清理并重试。")
                frameStatus != "missing" && (frameWidth <= 1 || frameHeight <= 1) ->
                    onProblem("NGA 登录 iframe 已创建且内容已加载，但显示尺寸仍然异常。请复制诊断报告。")
            }
        }
    }
}

@Composable
private fun WebLoginDiagnosticDialog(
    stage: String,
    events: List<String>,
    onCleanRetry: () -> Unit,
    onCopy: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val report = remember(stage, events.toList()) {
        buildString {
            appendLine("LNGA 网页登录诊断")
            appendLine("stage=$stage")
            appendLine("credential-values=redacted")
            events.forEach(::appendLine)
        }.trimEnd()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("网页登录诊断") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("当前状态：$stage", style = MaterialTheme.typography.titleSmall)
                Text(
                    "报告只包含 Cookie 是否存在，不包含 UID、CID 或 Cookie 值。请在空白页面停留约 7 秒后再复制。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SelectionContainer {
                    Text(
                        report,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                OutlinedButton(onClick = onCleanRetry, modifier = Modifier.fillMaxWidth()) {
                    Text("清理 NGA 网页状态并重试")
                }
            }
        },
        confirmButton = { TextButton(onClick = { onCopy(report) }) { Text("复制报告") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

private fun decodeJavascriptString(raw: String): String = runCatching {
    JSONObject("{\"value\":$raw}").getString("value")
}.getOrDefault(raw.trim('"'))

internal fun safeWebUrl(url: String?): String = runCatching {
    if (url.isNullOrBlank()) return "none"
    val uri = java.net.URI(url)
    "${uri.scheme}://${uri.host}${uri.path.orEmpty()}"
}.getOrElse { "invalid-url" }

private fun webLoginEnvironment(context: Context): String {
    val webViewVersion = WebView.getCurrentWebViewPackage()?.versionName ?: "unknown"
    val network = runCatching {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork)
        val transports = buildList {
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) add("wifi")
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) add("cellular")
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) add("vpn")
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true) add("ethernet")
        }.ifEmpty { listOf("unknown") }
        "network=${transports.joinToString("+")} validated=${capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)}"
    }.getOrElse { "network=unavailable(${it.javaClass.simpleName})" }
    return "app=${BuildConfig.VERSION_NAME} android=${Build.VERSION.RELEASE}/api${Build.VERSION.SDK_INT} " +
        "device=${Build.MANUFACTURER}/${Build.MODEL} webview=$webViewVersion $network"
}

internal fun isNgaLoginFrameUrl(url: String?): Boolean = runCatching {
    val uri = java.net.URI(url ?: return false)
    uri.host in NGA_HOSTS && uri.path?.endsWith("/nuke/account_copy.html") == true
}.getOrDefault(false)

internal fun parseNgaCookies(cookieHeader: String?): Pair<String, String>? {
    if (cookieHeader.isNullOrBlank()) return null
    var uid = ""
    var cid = ""
    cookieHeader.split(';').forEach { rawPart ->
        val part = rawPart.trim()
        when {
            part.startsWith("ngaPassportUid=") -> uid = part.substringAfter('=')
            part.startsWith("ngaPassportCid=") -> cid = part.substringAfter('=')
        }
    }
    return if (uid.isNotBlank() && cid.isNotBlank()) uid to cid else null
}
