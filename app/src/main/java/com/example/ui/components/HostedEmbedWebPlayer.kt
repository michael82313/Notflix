package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HostedEmbedWebPlayer(
    embedUrl: String,
    providerName: String,
    reloadToken: Int,
    modifier: Modifier = Modifier,
    onLoaded: () -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val latestOnLoaded by rememberUpdatedState(onLoaded)
    val latestOnError by rememberUpdatedState(onError)

    var customFullscreenView by remember(embedUrl, reloadToken) { mutableStateOf<View?>(null) }
    var customViewCallback by remember(embedUrl, reloadToken) { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val webView = remember(embedUrl, reloadToken) {
        WebView(context).apply {
            val webViewInstance = this
            setBackgroundColor(android.graphics.Color.BLACK)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(webViewInstance, true)
            }

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false
                loadsImagesAutomatically = true
                useWideViewPort = true
                loadWithOverviewMode = true
                javaScriptCanOpenWindowsAutomatically = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }

            webChromeClient = object : WebChromeClient() {
                override fun getDefaultVideoPoster(): Bitmap {
                    return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                }

                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    if (view == null) {
                        callback?.onCustomViewHidden()
                        return
                    }
                    customViewCallback?.onCustomViewHidden()
                    customFullscreenView = view
                    customViewCallback = callback
                }

                override fun onHideCustomView() {
                    customFullscreenView = null
                    customViewCallback?.onCustomViewHidden()
                    customViewCallback = null
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri = request?.url ?: return false
                    val scheme = uri.scheme?.lowercase().orEmpty()
                    if (scheme != "https" && scheme != "http") {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }
                        return true
                    }
                    return false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    latestOnLoaded()
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) {
                        val desc = error?.description?.toString()?.takeIf { it.isNotBlank() }
                            ?: "Unable to connect to $providerName"
                        Log.e("HostedEmbedWebPlayer", "Embed error for $providerName ($embedUrl): $desc")
                        latestOnError(
                            "$providerName player could not load ($desc). Tap 'Open Website' to watch directly in your browser."
                        )
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: android.webkit.WebResourceResponse?
                ) {
                    if (request?.isForMainFrame == true) {
                        val code = errorResponse?.statusCode ?: 0
                        if (code >= 400) {
                            Log.e("HostedEmbedWebPlayer", "HTTP $code from $providerName ($embedUrl)")
                            val reason = when (code) {
                                401, 403 -> "$providerName rejected embedded playback (HTTP $code). This video may be private or require sign-in on their site."
                                404 -> "$providerName reported this video was not found (HTTP 404)."
                                else -> "$providerName returned HTTP error $code."
                            }
                            latestOnError("$reason Tap 'Open Website' to open this source directly.")
                        }
                    }
                }
            }

            loadUrl(
                embedUrl,
                mapOf("Referer" to "https://com.aistudio.velora.kxmpzq/")
            )
        }
    }

    DisposableEffect(webView) {
        onDispose {
            runCatching {
                customViewCallback?.onCustomViewHidden()
                customFullscreenView = null
                customViewCallback = null
                webView.stopLoading()
                webView.loadUrl("about:blank")
                webView.destroy()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize()
        )

        customFullscreenView?.let { fsView ->
            AndroidView(
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        setBackgroundColor(android.graphics.Color.BLACK)
                        (fsView.parent as? ViewGroup)?.removeView(fsView)
                        addView(
                            fsView,
                            ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
