package com.github.swipehistorynavigation.sample

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.swipehistorynavigation.SwipeHistoryNavigationLayout

class MainActivity : AppCompatActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        val webView: WebView = findViewById(R.id.webview)
        webView.settings.apply {
            this.javaScriptEnabled = true
        }

        val swipeRefreshLayout: SwipeRefreshLayout = findViewById(R.id.swiperefresh)
        swipeRefreshLayout.let {
            it.setOnRefreshListener {
                webView.reload()
                it.isRefreshing = false
            }
        }
        val swipeHistoryNavigationLayout: SwipeHistoryNavigationLayout =
            findViewById(R.id.swipehistorynav)
        swipeHistoryNavigationLayout.listener = object : SwipeHistoryNavigationLayout.OnListener {
            override fun canSwipeLeftEdge(): Boolean = true
            override fun canSwipeRightEdge(): Boolean = webView.canGoForward()
            override fun goBack(): Boolean {
                Log.d("MainActivity", "goBack: canGoBack=${webView.canGoBack()}")
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    onBackPressed()
                }
                return true
            }

            override fun goForward(): Boolean {
                Log.d("MainActivity", "goForward: canGoForward=${webView.canGoForward()}")
                webView.goForward()
                return true
            }

            override fun getGoBackLabel(): String {
                Log.d("MainActivity", "getGoBackLabel: ${webView.canGoBack()}")
                if (!webView.canGoBack()) {
                    return "Close App"
                }
                return ""
            }

            override fun leftSwipeReachesLimit() = vibrator.vibrateShort()
            override fun rightSwipeReachesLimit() = vibrator.vibrateShort()
        }
        webView.loadUrl("https://material-ui.com/")
    }
}

fun Vibrator.vibrateShort() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val effect =
            VibrationEffect.createOneShot(10, 1)
        vibrate(effect)
    } else {
        @Suppress("DEPRECATION")
        vibrate(10)
    }
}