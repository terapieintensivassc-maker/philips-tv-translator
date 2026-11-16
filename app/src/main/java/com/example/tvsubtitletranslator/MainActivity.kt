package com.example.tvsubtitletranslator

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.*
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity(), MicCapture.Listener {
    private lateinit var web: WebView
    private lateinit var controls: LinearLayout
    private lateinit var subtitle: TextView
    private lateinit var status: TextView
    private lateinit var keyInput: EditText
    private lateinit var addressInput: EditText
    private lateinit var mic: MicCapture
    private val requestMic = 7201
    private val prefs by lazy { getSharedPreferences("translator_v2", MODE_PRIVATE) }
    private val cues = StringBuilder()
    private var cueNumber = 1
    private var sessionStart = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        mic = MicCapture(this, this)
        buildUi()
        openSite("https://cinejoy.pk/")
    }

    private fun buildUi() {
        val root = FrameLayout(this).apply { setBackgroundColor(0xFF000000.toInt()) }
        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.loadsImagesAutomatically = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri = request?.url ?: return false
                    return if (uri.scheme == "https" || uri.scheme == "http") {
                        view?.loadUrl(uri.toString()); true
                    } else true
                }
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    if (request?.isForMainFrame == true) showStatus("Pagina nu s-a încărcat: ${error?.description}")
                }
            }
            webChromeClient = WebChromeClient()
            isFocusable = true
        }
        root.addView(web, FrameLayout.LayoutParams(-1, -1))

        controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(14, 8, 14, 8)
            setBackgroundColor(0xDD101820.toInt())
        }
        val top = FrameLayout.LayoutParams(-1, dp(64), Gravity.TOP)
        root.addView(controls, top)
        controls.addView(makeButton("ÎNAPOI") { if (web.canGoBack()) web.goBack() })
        controls.addView(makeButton("CINEJOY") { openSite("https://cinejoy.pk/") })
        controls.addView(makeButton("CINECAT") { openSite("https://cinecat.eu/") })
        controls.addView(makeButton("START RO") { requestMicrophone() })
        controls.addView(makeButton("STOP") { mic.stop(); showStatus("Captură oprită") })
        controls.addView(makeButton("SRT") { exportSrt() })
        addressInput = EditText(this).apply {
            hint = "Introdu adresa site-ului (https://...)"
            singleLine = true
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFFCCCCCC.toInt())
            setPadding(12, 0, 12, 0)
            setText("https://cinejoy.pk/")
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                    openSite(addressInput.text.toString()); true
                } else false
            }
        }
        controls.addView(addressInput, LinearLayout.LayoutParams(dp(300), -1))
        controls.addView(makeButton("DESCHIDE") { openSite(addressInput.text.toString()) })

        subtitle = TextView(this).apply {
            text = "Subtitrările în română vor apărea aici"
            textSize = 25f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(20, 12, 20, 12)
            setBackgroundColor(0xCC000000.toInt())
        }
        val subParams = FrameLayout.LayoutParams(-1, dp(92), Gravity.BOTTOM).apply {
            leftMargin = dp(30); rightMargin = dp(30); bottomMargin = dp(26)
        }
        root.addView(subtitle, subParams)
        status = TextView(this).apply {
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xAA000000.toInt())
            setPadding(12, 6, 12, 6)
        }
        root.addView(status, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.LEFT))
        setContentView(root)
        showStatus("Deschide filmul. Pentru traducere este necesar un microfon conectat la TV.")
    }

    private fun makeButton(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text
        textSize = 13f
        isFocusable = true
        setOnClickListener { action() }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun openSite(rawUrl: String) {
        var url = rawUrl.trim()
        if (url.isBlank()) {
            showStatus("Introdu adresa unui site.")
            return
        }
        if (!url.contains("://")) url = "https://$url"
        val parsed = Uri.parse(url)
        if (parsed.scheme != "https" && parsed.scheme != "http") {
            showStatus("Sunt acceptate doar adrese web HTTP/HTTPS.")
            return
        }
        if (::addressInput.isInitialized) addressInput.setText(url)
        web.loadUrl(url)
    }

    private fun requestMicrophone() {
        if (prefs.getString("api_key", "").isNullOrBlank()) {
            showStatus("Configurează cheia API în Setări înainte de utilizare.")
            showSettings()
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), requestMic)
        } else startMic()
    }

    private fun showSettings() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
            setBackgroundColor(0xFF18232E.toInt())
        }
        val title = TextView(this).apply {
            text = "Configurare OpenAI API"
            textSize = 22f; setTextColor(-1)
        }
        keyInput = EditText(this).apply {
            hint = "API key (sk-...)"; setSingleLine(true); inputType = 129
            setTextColor(-1); setHintTextColor(0xFFBBBBBB.toInt())
            setText(prefs.getString("api_key", ""))
        }
        val save = makeButton("SALVEAZĂ") {
            prefs.edit().putString("api_key", keyInput.text.toString().trim()).apply()
            (box.parent as? android.view.ViewGroup)?.removeView(box)
            showStatus("Cheie salvată local.")
        }
        box.addView(title); box.addView(keyInput); box.addView(save)
        val overlay = FrameLayout(this)
        overlay.setBackgroundColor(0xEE000000.toInt())
        overlay.addView(box, FrameLayout.LayoutParams(dp(620), -2, Gravity.CENTER))
        (window.decorView as android.view.ViewGroup).addView(overlay)
        box.tag = overlay
        save.setOnClickListener {
            prefs.edit().putString("api_key", keyInput.text.toString().trim()).apply()
            (overlay.parent as? android.view.ViewGroup)?.removeView(overlay)
            showStatus("Cheie salvată local.")
        }
    }

    private fun startMic() {
        cues.clear(); cueNumber = 1
        sessionStart = System.currentTimeMillis()
        mic.start()
        showStatus("Microfon activ. Redă filmul și ține volumul TV moderat.")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == requestMic && results.firstOrNull() == PackageManager.PERMISSION_GRANTED) startMic()
        else showStatus("Permisiunea pentru microfon este necesară.")
    }

    override fun onTranscript(translated: String, startSec: Double, endSec: Double) {
        runOnUiThread {
            subtitle.text = translated
            synchronized(cues) {
                cues.append(cueNumber++).append('\n')
                    .append(stamp(startSec)).append(" --> ").append(stamp(endSec)).append('\n')
                    .append(translated).append("\n\n")
            }
        }
    }

    override fun onStatus(message: String) { runOnUiThread { showStatus(message) } }

    private fun stamp(seconds: Double): String {
        val ms = (seconds.coerceAtLeast(0.0) * 1000).toLong()
        return String.format(java.util.Locale.US, "%02d:%02d:%02d,%03d",
            ms / 3600000, (ms % 3600000) / 60000, (ms % 60000) / 1000, ms % 1000)
    }

    private fun exportSrt() {
        val text = synchronized(cues) { cues.toString() }
        if (text.isBlank()) { showStatus("Nu există subtitrări încă."); return }
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type = "application/x-subrip"
            putExtra(Intent.EXTRA_TITLE, "cinejoy_ro.srt")
        }
        startActivityForResult(intent, 7202)
    }

    @Deprecated("Deprecated Android callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 7202 && resultCode == RESULT_OK) {
            data?.data?.let { uri: Uri ->
                contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(synchronized(cues) { cues.toString() }.toByteArray(Charsets.UTF_8))
                }
                showStatus("SRT salvat.")
            }
        }
    }

    private fun showStatus(text: String) { if (::status.isInitialized) status.text = text }

    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        mic.stop()
        web.destroy()
        super.onDestroy()
    }
}
