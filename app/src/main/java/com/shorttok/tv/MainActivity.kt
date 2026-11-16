package com.shorttok.tv

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var hintOverlay: TextView

    companion object {
        private const val START_URL = "https://www.tiktok.com/foryou"
        private const val DESKTOP_UA =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableImmersiveMode()

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        webView = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                userAgentString = DESKTOP_UA
                loadWithOverviewMode = true
                useWideViewPort = true
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    injectTvStyles()
                }
            }
        }

        hintOverlay = TextView(this).apply {
            text = "▲▼ Næste/Forrige  •  OK Play/Pause  •  ▶ Like"
            setTextColor(Color.WHITE)
            setBackgroundColor(0xAA000000.toInt())
            setPadding(32, 16, 32, 16)
            textSize = 14f
        }

        val hintParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        ).apply {
            bottomMargin = 36
        }

        root.addView(webView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        root.addView(hintOverlay, hintParams)
        setContentView(root)

        webView.loadUrl(START_URL)

        // Fade out remote hint after 6 seconds
        hintOverlay.postDelayed({
            hintOverlay.animate().alpha(0f).setDuration(600).withEndAction {
                hintOverlay.visibility = View.GONE
            }.start()
        }, 6000)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                    sendJsKey("ArrowDown", 40)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_CHANNEL_UP -> {
                    sendJsKey("ArrowUp", 38)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    togglePlayPause()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    sendJsKey("l", 76)
                    return true
                }
                KeyEvent.KEYCODE_BACK -> {
                    if (webView.canGoBack()) {
                        webView.goBack()
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun sendJsKey(key: String, keyCode: Int) {
        val js = """
            (function() {
                const ev = new KeyboardEvent('keydown', {
                    key: '$key',
                    code: '$key',
                    keyCode: $keyCode,
                    which: $keyCode,
                    bubbles: true,
                    cancelable: true
                });
                document.dispatchEvent(ev);
                window.dispatchEvent(ev);
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun togglePlayPause() {
        val js = """
            (function() {
                const videos = Array.from(document.querySelectorAll('video'));
                const playing = videos.find(v => !v.paused);
                if (playing) {
                    playing.pause();
                } else if (videos.length > 0) {
                    videos[0].play().catch(() => {});
                }
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun injectTvStyles() {
        val js = """
            (function() {
                if (document.getElementById('shorttok-tv-style')) return;
                const style = document.createElement('style');
                style.id = 'shorttok-tv-style';
                style.textContent = 'body { overflow: hidden !important; background: #000 !important; }';
                document.head.appendChild(style);
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun enableImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
