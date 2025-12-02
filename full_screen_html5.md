Android WebView 默认确实不支持全屏播放 HTML5 视频，这与 iOS 的 `WKWebView` 开箱即用不同。在 Android 上，你需要手动“接管”全屏事件。

实现的核心在于自定义 `WebChromeClient`，并重写 `onShowCustomView`（进入全屏）和 `onHideCustomView`（退出全屏）这两个方法。

这里是一个完整的、现代化的 Kotlin 代码指南，涵盖了 Manifest 配置、布局 XML 和 Activity 代码。

### 1\. 修改 AndroidManifest.xml

你需要做两件事：

1.  **开启硬件加速**：视频播放必须开启硬件加速。
2.  **处理配置变更**：防止旋转屏幕时 Activity 重启导致视频中断。

<!-- end list -->

```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:hardwareAccelerated="true"
    android:configChanges="orientation|screenSize|keyboardHidden|smallestScreenSize|screenLayout">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

-----

### 2\. 修改布局文件 (activity\_main.xml)

你需要一个全屏的容器（通常是 `FrameLayout`），用于在全屏模式下覆盖整个屏幕显示视频。

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <FrameLayout
        android:id="@+id/webViewContainer"
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <WebView
            android:id="@+id/webView"
            android:layout_width="match_parent"
            android:layout_height="match_parent" />
    </FrameLayout>

    <FrameLayout
        android:id="@+id/fullScreenVideoContainer"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:visibility="gone"
        android:background="@android:color/black"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        android:elevation="10dp" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

-----

### 3\. Kotlin 完整实现 (MainActivity.kt)

这段代码处理了全屏切换、隐藏系统 UI（状态栏）、以及物理返回键的逻辑。

```kotlin
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var webViewContainer: FrameLayout
    private lateinit var fullScreenContainer: FrameLayout

    // 用于全屏播放的 View 和回调
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        webViewContainer = findViewById(R.id.webViewContainer)
        fullScreenContainer = findViewById(R.id.fullScreenVideoContainer)

        setupWebView()
        handleBackPressed()
    }

    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false // 允许自动播放（可选）

        webView.webViewClient = WebViewClient()
        
        // 关键：设置自定义 WebChromeClient
        webView.webChromeClient = object : WebChromeClient() {
            
            // 进入全屏时调用
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) {
                    callback?.onCustomViewHidden()
                    return
                }

                customView = view
                customViewCallback = callback

                // 1. 隐藏 WebView，显示全屏容器
                webViewContainer.visibility = View.GONE
                fullScreenContainer.visibility = View.VISIBLE
                fullScreenContainer.addView(view)

                // 2. 隐藏系统状态栏 (沉浸式体验)
                hideSystemUI()

                // 3. (可选) 强制横屏
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }

            // 退出全屏时调用
            override fun onHideCustomView() {
                if (customView == null) return

                // 1. 恢复显示 WebView
                fullScreenContainer.removeView(customView)
                fullScreenContainer.visibility = View.GONE
                webViewContainer.visibility = View.VISIBLE

                // 2. 清理资源
                customView = null
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null

                // 3. 恢复系统状态栏
                showSystemUI()

                // 4. 恢复竖屏（或用户之前的方向）
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT // 或者 USER
            }
        }

        // 加载测试视频网页（例如 YouTube 或你的 H5 页面）
        webView.loadUrl("https://www.youtube.com") 
    }

    // 处理返回键：如果是全屏模式，先退出全屏；否则执行浏览器后退
    private fun handleBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (customView != null) {
                    // 如果在全屏播放，退出全屏
                    webView.webChromeClient?.onHideCustomView()
                } else if (webView.canGoBack()) {
                    // 如果网页能后退，则后退
                    webView.goBack()
                } else {
                    // 否则退出 Activity
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    // 辅助方法：隐藏系统 UI
    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    // 辅助方法：显示系统 UI
    private fun showSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
    }
    
    // 生命周期管理：销毁时清理 WebView，防止内存泄漏
    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
    
    // 处理暂停/恢复，确保视频在后台不继续播放声音
    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }
}
```

### 关键点总结 (Troubleshooting)

1.  **`AndroidManifest.xml` 中的 `configChanges`**：

      * 如果不加这个属性，当用户全屏播放视频并旋转手机时，Activity 会重建，导致网页重新加载，视频中断。这是一个非常常见的坑。

2.  **返回键处理**：

      * 如果不拦截返回键，用户点击“返回”时会直接退出当前 Activity，而不是退出视频全屏。代码中使用了 `OnBackPressedDispatcher` 来优雅处理。

3.  **`View.GONE` 与 `View.VISIBLE`**：

      * 在全屏时，建议将原来的 `WebView` 所在的容器设为 `GONE`，只显示全屏容器。这能避免由于布局重叠导致的触摸冲突或渲染问题。

4.  **强制横屏 (`requestedOrientation`)**：

      * 代码示例中包含了一行强制横屏的代码。如果你希望用户全屏时仍然可以保持竖屏（例如播放竖屏短视频），可以删除 `requestedOrientation = ...` 这行代码。

Would you like me to create a helper class to encapsulate this logic if you have multiple WebViews in your project?