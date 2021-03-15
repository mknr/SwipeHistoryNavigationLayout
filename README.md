# SwipeHistoryNavigationLayout

SwipeHistoryNavigationLayout provides the same functionality to WebView as overcroll-history-navigation in Chrome for Android.

## Usage

### Basic(use with WebView)

layout xml is
```xml
<com.github.swipehistorynavigation.SwipeHistoryNavigationLayout
    android:id="@+id/swipehistorynav"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    <WebView
        android:id="@+id/webview"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
</com.github.swipehistorynavigation.SwipeHistoryNavigationLayout>
```

Implement a listener and connect it to the webview.
```kotlin
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
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    onBackPressed()
                }
                return true
            }

            override fun goForward(): Boolean {
                webView.goForward()
                return true
            }

            override fun getGoBackLabel(): String {
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
```

### Customize Style
```xml
<com.github.swipehistorynavigation.SwipeHistoryNavigationLayout
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/swipehistorynav"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:handleBackground="@drawable/shape_rounded"
    app:leftHandleLabel="default text"
    app:activeColor="#ffff0000"
    app:inactiveColor="#ff00ff00"
    app:leftHandleDrawable="@drawable/ic_baseline_arrow_back_24"
    app:rightHandleDrawable="@drawable/ic_baseline_arrow_forward_24">

</com.github.swipehistorynavigation.SwipeHistoryNavigationLayout>
```

### Use with SwipeRefreshLayout

It is possible to use it together with SwipeRefreshLayout

```xml
<androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    android:id="@+id/swiperefresh"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    <com.github.swipehistorynavigation.SwipeHistoryNavigationLayout
        android:id="@+id/swipehistorynav"
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <WebView
            android:id="@+id/webview"
            android:layout_width="match_parent"
            android:layout_height="match_parent" />
    </com.github.swipehistorynavigation.SwipeHistoryNavigationLayout>
</androidx.swiperefreshlayout.widget.SwipeRefreshLayout>
```

## Installation

Add it in your root build.gradle at the end of repositories:

```groovy
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

Step 2. Add the dependency

${latest.version} is [![](https://jitpack.io/v/mknr/SwipeHistoryNavigationLayout.svg)](https://jitpack.io/#mknr/SwipeHistoryNavigationLayout)

```groovy
	dependencies {
	        implementation "com.github.mknr:SwipeHistoryNavigationLayout:${latest.version}"
	}
```

## License

```
Copyright 2021 Noriaki Maki
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
   http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```