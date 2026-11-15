package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
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

    // Safety timeout if YouTube IFrame fails to become ready (e.g. blocked/offline)
    LaunchedEffect(videoId, reloadToken) {
        delay(16_000L)
        if (!playerReady) {
            latestOnError(
                "YouTube player timed out while loading. Verify network connectivity and confirm embedding is permitted for this video."
            )
        }
    }

    val webView = remember(videoId, reloadToken) {
        WebView(context).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                loadsImagesAutomatically = true
                useWideViewPort = true
                loadWithOverviewMode = true
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            }

            val bridge = object {
                @JavascriptInterface
                fun onPlayerReady(durationSeconds: Double) {
                    mainHandler.post {
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
                        val friendly = when (code) {
                            2 -> "Invalid YouTube video ID or embed parameter."
                            5 -> "This YouTube video cannot play in the embedded HTML5 player."
                            100 -> "This YouTube video was removed or marked private."
                            101, 150, 153 -> "Embedding is disabled by the owner of this YouTube video (Error $code)."
                            else -> "YouTube playback unavailable (Error $code)."
                        }
                        latestOnError(friendly)
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
                        latestOnError(
                            error?.description?.toString()?.takeIf { it.isNotBlank() }
                                ?: "Unable to connect to YouTube embedded player."
                        )
                    }
                }
            }

            val startSeconds = (initialPositionMs / 1000L).coerceAtLeast(0L)
            val html = buildYouTubeIframeHtml(videoId = videoId, startSeconds = startSeconds)
            loadDataWithBaseURL(
                "https://www.youtube.com",
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

private fun buildYouTubeIframeHtml(videoId: String, startSeconds: Long): String {
    val safeVideoId = videoId.replace(Regex("[^a-zA-Z0-9_-]"), "")
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
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
              player = new YT.Player('player', {
                width: '100%',
                height: '100%',
                videoId: '$safeVideoId',
                playerVars: {
                  autoplay: 1,
                  controls: 1,
                  playsinline: 1,
                  rel: 0,
                  modestbranding: 1,
                  iv_load_policy: 3,
                  enablejsapi: 1,
                  origin: 'https://www.youtube.com',
                  start: $startSeconds
                },
                events: {
                  onReady: function(event) {
                    try {
                      var dur = event.target.getDuration() || 0;
                      NotflixYouTubeBridge.onPlayerReady(dur);
                      event.target.playVideo();
                    } catch (e) {}
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
                      NotflixYouTubeBridge.onPlayerError(event.data || -1);
                    } catch (e) {}
                  }
                }
              });
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
