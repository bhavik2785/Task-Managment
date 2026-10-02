package com.mechvac.task

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    companion object {
        const val EXTRA_PATH = "path"
        private const val REQ_NOTIF = 3
        private const val REQ_LAUNCH_NOTIF = 5
        private const val REQ_FILE = 9
        /** True while the app is on screen. */
        @Volatile var visible = false
        /** The page open in the app, so a chat you are already reading does not notify you. */
        @Volatile var currentUrl = ""
    }

    private var web: WebView? = null
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var permStep = 99
    private var batteryPending = false
    private var settingsPending = false
    private var notifTried = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notify.channels(this)
        if (Prefs(this).baseUrl.isBlank()) showSetup() else openWeb(intent?.getStringExtra(EXTRA_PATH))
        // Ask for notifications straight away (Android 13+), before sign-in
        if (Build.VERSION.SDK_INT >= 33 && !Perms.hasNotifications(this) && !Prefs(this).notifAskedAtLaunch) {
            Prefs(this).notifAskedAtLaunch = true
            notifTried = true
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_LAUNCH_NOTIF)
        }
    }

    // ---------- first launch, when no address was built into the app ----------
    private fun showSetup() {
        val pad = (24 * resources.displayMetrics.density).toInt()
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(Color.parseColor("#EFF5F7"))
        }
        val title = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 28f
            setTextColor(Color.parseColor("#0097B2"))
        }
        val hint = TextView(this).apply {
            text = "Enter your company’s MechVac Task website address."
            textSize = 16f
            setTextColor(Color.parseColor("#12303A"))
            setPadding(0, pad / 2, 0, pad / 2)
        }
        val input = EditText(this).apply {
            this.hint = "https://task.yourcompany.com"
            inputType = InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine()
        }
        val go = Button(this).apply { text = "Continue" }
        go.setOnClickListener {
            var url = input.text.toString().trim().trimEnd('/')
            if (url.isNotEmpty() && !url.startsWith("http")) url = "https://$url"
            if (!Regex("^https?://[^\\s/]+.*$").matches(url)) {
                Toast.makeText(this, "Please enter a valid address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Prefs(this).baseUrl = url
            openWeb(null)
        }
        box.addView(title); box.addView(hint)
        box.addView(input, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        box.addView(go, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(box)
    }

    // ---------- the MechVac Task website inside the app ----------
    @SuppressLint("SetJavaScriptEnabled")
    private fun openWeb(path: String?) {
        val base = Prefs(this).baseUrl
        val baseHost = Uri.parse(base).host
        val w = WebView(this)
        web = w
        setContentView(w)
        w.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            userAgentString = "$userAgentString MechVacTaskApp/${BuildConfig.VERSION_NAME}"
        }
        CookieManager.getInstance().setAcceptCookie(true)
        w.addJavascriptInterface(Bridge(this), "MechVacApp")

        w.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url
                // our own pages stay inside the app; WhatsApp, phone calls, maps etc. open in their apps
                if ((url.scheme == "https" || url.scheme == "http") && url.host == baseHost) return false
                try { startActivity(Intent(Intent.ACTION_VIEW, url)) } catch (e: Exception) { }
                return true
            }

            override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                currentUrl = url ?: ""
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) view.loadDataWithBaseURL(null, offlinePage(base), "text/html", "utf-8", null)
            }
        }

        w.webChromeClient = object : WebChromeClient() {
            // profile photos, task attachments and chat files
            override fun onShowFileChooser(view: WebView, callback: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback
                return try {
                    startActivityForResult(params.createIntent(), REQ_FILE)
                    true
                } catch (e: Exception) {
                    fileCallback = null
                    Toast.makeText(this@MainActivity, "No app found to choose files", Toast.LENGTH_SHORT).show()
                    false
                }
            }
        }

        // documents and files from tasks and chat are saved to Downloads
        w.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            try {
                val name = URLUtil.guessFileName(url, contentDisposition, mimeType)
                val req = DownloadManager.Request(Uri.parse(url))
                    .setTitle(name)
                    .setMimeType(mimeType)
                    .addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url) ?: "")
                    .addRequestHeader("User-Agent", userAgent)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
                getSystemService(DownloadManager::class.java)?.enqueue(req)
                Toast.makeText(this, "Downloading $name", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e2: Exception) { }
            }
        }
        w.loadUrl(base + (path ?: "/dashboard.php"))
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION") super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_FILE) {
            fileCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data))
            fileCallback = null
        }
    }

    private fun offlinePage(base: String) = """
        <html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head>
        <body style="font-family:sans-serif;background:#EFF5F7;color:#12303A;display:flex;align-items:center;justify-content:center;height:90vh;text-align:center;padding:20px">
        <div><div style="font-size:48px">📶</div><h2>No internet</h2><p>Check your mobile data or Wi-Fi.</p>
        <a href="$base/dashboard.php" style="display:inline-block;margin-top:12px;background:#0097B2;color:#fff;padding:12px 24px;border-radius:10px;text-decoration:none;font-weight:bold">Try again</a></div></body></html>
    """.trimIndent()

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val path = intent.getStringExtra(EXTRA_PATH) ?: return
        web?.loadUrl(Prefs(this).baseUrl + path)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val w = web
        if (w != null && w.canGoBack()) w.goBack() else @Suppress("DEPRECATION") super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        visible = true
        if (Prefs(this).token != null) {
            SyncJob.schedule(this)
            LiveService.start(this)   // only if the admin switched on the always-on check
            val app = applicationContext
            Thread { Feed.run(app) }.start()   // fresh settings, Firebase token, and anything missed
        }
        if (settingsPending) {
            settingsPending = false
            permStep++
            nextPermission()
        } else if (batteryPending) {
            batteryPending = false
            permStep = 2
            nextPermission()
        }
    }

    override fun onPause() {
        super.onPause()
        visible = false
        CookieManager.getInstance().flush()
    }

    // ---------- called when the website links this phone to the signed-in person ----------
    fun onLinked() {
        SyncJob.schedule(this)
        val app = applicationContext
        Thread { Feed.run(app) }.start()   // gets the Firebase settings and registers this phone for instant alerts
        askPermissions(force = false)
    }

    fun openNotificationSettings() {
        try {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        } catch (e: Exception) { openAppSettings() }
    }

    fun openAppSettings() {
        try { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) } catch (e: Exception) { }
    }

    // ---------- notifications, battery, autostart: one step at a time with a short explanation ----------
    fun askPermissions(force: Boolean) {
        val p = Prefs(this)
        if (!force && p.permsAsked) return
        p.permsAsked = true
        if (force) { p.autostartShown = false; notifTried = false }
        permStep = 0
        nextPermission()
    }

    private fun nextPermission() {
        while (permStep < 3) {
            when (permStep) {
                0 -> if (!Perms.hasNotifications(this)) {
                    if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED && !notifTried) {
                        notifTried = true
                        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIF)
                    } else {
                        explain("Turn on notifications", "Notifications are off for this app. On the next screen, switch on “Allow notifications”, then come back.") {
                            settingsPending = true
                            openNotificationSettings()
                        }
                    }
                    return
                }
                1 -> if (!Perms.batteryOk(this)) {
                    explain("Get alerts without delay", "Some phones stop apps to save battery, which delays task and chat notifications. Tap “Allow” on the next screen.") {
                        batteryPending = true
                        try {
                            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
                        } catch (e: Exception) {
                            try { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) } catch (e2: Exception) { batteryPending = false; permStep = 2; nextPermission() }
                        }
                    }
                    return
                }
                2 -> if (Autostart.needed() && !Prefs(this).autostartShown) {
                    explain("Allow Autostart", Autostart.instructions()) {
                        Prefs(this).autostartShown = true
                        settingsPending = true
                        if (!Autostart.open(this)) { settingsPending = false; openAppSettings(); permStep++ }
                    }
                    return
                }
            }
            permStep++
        }
        finishPermissions()
    }

    private fun finishPermissions() {
        permStep = 99
        SyncJob.schedule(this)
        LiveService.start(this)
        if (Perms.hasNotifications(this)) Toast.makeText(this, "All set. You will get notifications even when the app is closed.", Toast.LENGTH_LONG).show()
        web?.evaluateJavascript("document.dispatchEvent(new Event('visibilitychange'))", null)
    }

    private fun explain(title: String, message: String, onOk: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Continue") { _, _ -> onOk() }
            .setNegativeButton("Not now") { _, _ -> permStep++; nextPermission() }
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_LAUNCH_NOTIF) return
        if (requestCode == REQ_NOTIF && permStep < 3) {
            // if Android refused without a popup (refused before), the same step now opens Settings instead
            if (Perms.hasNotifications(this)) permStep++
            nextPermission()
        }
    }
}
