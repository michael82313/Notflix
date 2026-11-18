package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeEmbedPlayer(
    videoId: String,
    initialPositionMs: Long,
    reloadToken: Int,
    modifier: Modifier = Modifier,
    onReady: (durationMs: Long) -> Unit,
    onProgress: (positionMs: Long, durationMs: Long, isPlaying: Boolean) -> Unit,
    onEnded: (durationMs: Long) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val latestOnReady by rememberUpdatedState(onReady)
    val latestOnProgress by rememberUpdatedState(onProgress)
    val latestOnEnded by rememberUpdatedState(onEnded)
    val latestOnError by rememberUpdatedState(onError)

    var playerReady by remember(videoId, reloadToken) { mutableStateOf(false) }

    val appId = remember(context) { context.packageName }
    val appOrigin = remember(appId) { "https://$appId" }
    val appBaseUrl = remember(appOrigin) { "$appOrigin/" }

    // Safety timeout if YouTube IFrame fails to become ready (e.g. blocked/offline)
    LaunchedEffect(videoId, reloadToken) {
        delay(16_000L)
        if (!playerReady) {
            Log.w(
                "YouTubeEmbedPlayer",
                "Playback timeout: YouTube IFrame player did not signal readiness (videoId=$videoId, origin=$appOrigin, baseUrl=$appBaseUrl)"
            )
            latestOnError(
                "YouTube player timed out while loading video '$videoId'. Check network connectivity or open the video on YouTube."
            )
        }
    }

    val webView = remember(videoId, reloadToken) {
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
                allowFileAccess = true
                mediaPlaybackRequiresUserGesture = false
                loadsImagesAutomatically = true
                useWideViewPort = true
                loadWithOverviewMode = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }

            val bridge = object {
                @JavascriptInterface
                fun onPlayerReady(durationSeconds: Double) {
                    mainHandler.post {
                        Log.i(
                            "YouTubeEmbedPlayer",
                            "IFrame API onReady: videoId=$videoId, duration=${durationSeconds}s, origin=$appOrigin"
                        )
                        playerReady = true
                        latestOnReady((durationSeconds * 1000.0).toLong().coerceAtLeast(0L))
                    }
                }

                @JavascriptInterface
                fun onPlayerState(state: Int, currentSeconds: Double, durationSeconds: Double) {
                    mainHandler.post {
                        val posMs = (currentSeconds * 1000.0).toLong().coerceAtLeast(0L)
                        val durMs = (durationSeconds * 1000.0).toLong().coerceAtLeast(0L)
                        val isPlaying = state == 1
                        latestOnProgress(posMs, durMs, isPlaying)
                        if (state == 0) { // YT.PlayerState.ENDED
                            latestOnEnded(durMs)
                        }
                    }
                }

                @JavascriptInterface
                fun onTimeUpdate(currentSeconds: Double, durationSeconds: Double) {
                    mainHandler.post {
                        val posMs = (currentSeconds * 1000.0).toLong().coerceAtLeast(0L)
                        val durMs = (durationSeconds * 1000.0).toLong().coerceAtLeast(0L)
                        latestOnProgress(posMs, durMs, true)
                    }
                }

                @JavascriptInterface
                fun onPlayerError(code: Int) {
                    mainHandler.post {
                        playerReady = true
                        Log.e(
                            "YouTubeEmbedPlayer",
                            "IFrame API onError: code=$code, videoId=$videoId, origin=$appOrigin, baseUrl=$appBaseUrl"
                        )
                        val diagnosis = when (code) {
                            2 -> "YouTube Error 2: Invalid video parameter or malformed video ID '$videoId'."
                            5 -> "YouTube Error 5: This video could not be played in the HTML5 player on this device."
                            100 -> "YouTube Error 100: Video not found. It may have been removed or set to private."
                            101, 150 -> "YouTube Error $code: Embedded playback was blocked by YouTube. This can happen when YouTube requires bot verification (\"Sign in to confirm you're not a bot\" on unsigned devices or cloud preview networks), due to age/region restrictions, or if embedding is disabled."
                            152, 153 -> "YouTube Error $code: Embedder identity or HTTP Referer validation failed for '$appOrigin'."
                            else -> "YouTube Player Error $code: Playback unavailable for video ID '$videoId'."
                        }
                        latestOnError(diagnosis)
                    }
                }
            }

            addJavascriptInterface(bridge, "NotflixYouTubeBridge")

            webChromeClient = object : WebChromeClient() {
                override fun getDefaultVideoPoster(): Bitmap {
                    return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url?.toString().orEmpty()
                    if (url.startsWith("https://www.youtube.com/watch") ||
                        url.startsWith("https://m.youtube.com/watch") ||
                        url.startsWith("https://youtu.be/")
                    ) {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                        return true
                    }
                    return false
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) {
                        playerReady = true
                        val errDesc = error?.description?.toString() ?: "Unknown WebView error"
                        Log.e("YouTubeEmbedPlayer", "WebView main frame error: $errDesc (url=${request.url})")
                        latestOnError("WebView connection error: $errDesc")
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: WebResourceResponse?
                ) {
                    if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 0) >= 400) {
                        playerReady = true
                        val status = errorResponse?.statusCode ?: 0
                        Log.e("YouTubeEmbedPlayer", "WebView HTTP error $status for videoId=$videoId (url=${request.url})")
                        latestOnError("YouTube returned HTTP $status while loading the player.")
                    }
                }

                override fun onRenderProcessGone(
                    view: WebView?,
                    detail: RenderProcessGoneDetail?
                ): Boolean {
                    val crashed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        detail?.didCrash() == true
                    } else {
                        false
                    }
                    Log.e("YouTubeEmbedPlayer", "WebView render process gone (didCrash=$crashed) for videoId=$videoId")
                    playerReady = true
                    runCatching {
                        (view?.parent as? ViewGroup)?.removeView(view)
                        view?.destroy()
                    }
                    latestOnError("The embedded YouTube video renderer stopped unexpectedly. Tap Retry to reload playback.")
                    return true
                }
            }

            val startSeconds = (initialPositionMs / 1000L).coerceAtLeast(0L)
            val html = buildYouTubeIframeHtml(
                videoId = videoId,
                startSeconds = startSeconds,
                appOrigin = appOrigin,
                widgetReferrer = appBaseUrl
            )
            loadDataWithBaseURL(
                appBaseUrl,
                html,
                "text/html",
                "utf-8",
                null
            )
        }
    }

    DisposableEffect(webView) {
        onDispose {
            runCatching {
                webView.evaluateJavascript("if (window.notflixStop) window.notflixStop();", null)
                webView.stopLoading()
                webView.removeJavascriptInterface("NotflixYouTubeBridge")
                webView.destroy()
            }
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    )
}

private fun buildYouTubeIframeHtml(
    videoId: String,
    startSeconds: Long,
    appOrigin: String,
    widgetReferrer: String
): String {
    val safeVideoId = videoId.replace(Regex("[^a-zA-Z0-9_-]"), "")
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
          <meta name="referrer" content="strict-origin-when-cross-origin" />
          <style>
            html, body {
              margin: 0;
              padding: 0;
              width: 100%;
              height: 100%;
              background-color: #000000;
              overflow: hidden;
            }
            #player-container {
              position: fixed;
              inset: 0;
              width: 100%;
              height: 100%;
              background: #000000;
            }
            iframe {
              width: 100%;
              height: 100%;
              border: 0;
            }
          </style>
        </head>
        <body>
          <div id="player-container">
            <div id="player"></div>
          </div>
          <script src="https://www.youtube.com/iframe_api"></script>
          <script>
            var player = null;
            var pollTimer = null;

            function onYouTubeIframeAPIReady() {
              try {
                player = new YT.Player('player', {
                  width: '100%',
                  height: '100%',
                  videoId: '$safeVideoId',
                  playerVars: {
                    autoplay: 1,
                    controls: 1,
                    playsinline: 1,
                    rel: 0,
                    iv_load_policy: 3,
                    enablejsapi: 1,
                    origin: '$appOrigin',
                    widget_referrer: '$widgetReferrer',
                    start: $startSeconds
                  },
                  events: {
                    onReady: function(event) {
                      try {
                        var dur = event.target.getDuration() || 0;
                        NotflixYouTubeBridge.onPlayerReady(dur);
                      } catch (e) {
                        console.error('onReady error:', e);
                      }
                    },
                    onStateChange: function(event) {
                      try {
                        var cur = event.target.getCurrentTime() || 0;
                        var dur = event.target.getDuration() || 0;
                        NotflixYouTubeBridge.onPlayerState(event.data, cur, dur);
                        if (event.data === 1) {
                          if (!pollTimer) {
                            pollTimer = setInterval(function() {
                              if (player && player.getCurrentTime) {
                                NotflixYouTubeBridge.onTimeUpdate(
                                  player.getCurrentTime() || 0,
                                  player.getDuration() || 0
                                );
                              }
                            }, 2000);
                          }
                        } else {
                          if (pollTimer) {
                            clearInterval(pollTimer);
                            pollTimer = null;
                          }
                        }
                      } catch (e) {}
                    },
                    onError: function(event) {
                      try {
                        console.error('YT.Player error event:', event.data);
                        NotflixYouTubeBridge.onPlayerError(event.data || -1);
                      } catch (e) {
                        NotflixYouTubeBridge.onPlayerError(-98);
                      }
                    }
                  }
                });
              } catch (err) {
                console.error('YT.Player init exception:', err);
                NotflixYouTubeBridge.onPlayerError(-99);
              }
            }

            window.notflixStop = function() {
              try {
                if (pollTimer) {
                  clearInterval(pollTimer);
                  pollTimer = null;
                }
                if (player && player.destroy) {
                  player.destroy();
                }
              } catch (e) {}
            };
          </script>
        </body>
        </html>
    """.trimIndent()
}
