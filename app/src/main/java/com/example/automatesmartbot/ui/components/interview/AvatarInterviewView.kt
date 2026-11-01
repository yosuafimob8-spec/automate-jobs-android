package com.example.automatesmartbot.ui.components.interview

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import org.json.JSONObject

/** Receives HR-avatar media only; candidate camera is never requested or evaluated. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AvatarInterviewView(subtitle: String, answerSdp: String?, iceServers: JSONArray?, onOffer: (String) -> Unit, onError: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val latestOffer by rememberUpdatedState(onOffer); val latestError by rememberUpdatedState(onError)
    var pageReady by remember { mutableStateOf(false) }
    val web = remember { WebView(context).apply {
        settings.javaScriptEnabled = true; settings.allowFileAccess = false; settings.allowContentAccess = false
        settings.mediaPlaybackRequiresUserGesture = false; settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
        addJavascriptInterface(object {
            @JavascriptInterface fun onOffer(sdp: String) { if (sdp.length <= 100000 && sdp.startsWith("v=0")) post { latestOffer(sdp) } }
            @JavascriptInterface fun onError(message: String) { post { latestError("Video: ${message.take(300)}") } }
        }, "KarirMate")
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean = true
            override fun onPageFinished(view: WebView?, url: String?) { pageReady = true; evaluateJavascript("connect(${iceServers ?: JSONArray()})", null) }
        }
        loadDataWithBaseURL("https://karirmate.invalid/", context.assets.open("interview/avatar.html").bufferedReader().use { it.readText() }, "text/html", "UTF-8", null)
    } }
    AndroidView(factory = { web }, modifier = modifier)
    LaunchedEffect(pageReady, subtitle) { if (pageReady) web.evaluateJavascript("setSubtitle(${JSONObject.quote(subtitle)})", null) }
    LaunchedEffect(pageReady, answerSdp) { if (pageReady && answerSdp != null) web.evaluateJavascript("acceptAnswer(${JSONObject.quote(answerSdp)})", null) }
    DisposableEffect(web) { onDispose { web.evaluateJavascript("closeSession()", null); web.removeJavascriptInterface("KarirMate"); web.destroy() } }
}
