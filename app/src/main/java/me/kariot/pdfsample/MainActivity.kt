package me.kariot.pdfsample

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.ybq.android.spinkit.SpinKitView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var webview: WebView
    private lateinit var loader: SpinKitView
    var reportLoadError = false
    var queId: Long? = null
    lateinit var receiver: BroadcastReceiver
    private lateinit var downloadManager: DownloadManager
    private val pdfUrl =
        "https://oui.doleta.gov/dmstree/handbooks/407/appendices_r1200/r1200appendix_d.pdf"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webview = findViewById(R.id.webview)
        loader = findViewById(R.id.loader)
        initWebView()
        initShare()
        initBroadcastReceiver()
        registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

    }

    private fun initBroadcastReceiver() {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                toast("On Receive")
                val action = p1!!.action
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
                    var requestQuery = DownloadManager.Query()
                    requestQuery.setFilterById(queId!!)
                    val cursor = downloadManager.query(requestQuery)
                    if (cursor.moveToFirst()) {
                        val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        if (DownloadManager.STATUS_SUCCESSFUL == cursor.getInt(columnIndex)) {
                            toast("Download Completed")
                            val uriString =
                                cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                            val uri = Uri.parse(uriString)
                            val share = Intent()
                            share.action = Intent.ACTION_SEND
                            share.type = "application/pdf"
                            share.putExtra(Intent.EXTRA_STREAM, uri)
                            startActivity(share)
                        } else if (DownloadManager.STATUS_FAILED == cursor.getInt(columnIndex)) {
                            toast("Download Failed")
                        }
                    }

                }
            }

        }
    }

    private fun initShare() {
        iconShare.setOnClickListener {
            toast("Downloading...")
            downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri =
                Uri.parse(pdfUrl)
            val request: DownloadManager.Request = DownloadManager.Request(uri)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            queId = downloadManager.enqueue(request)

        }
    }

    private fun initWebView() {
        reportLoadError = false
        loader.visibility = View.VISIBLE
        noData.visibility = View.GONE
        webview.webViewClient = object : WebViewClient() {
            override fun onPageStarted(webview: WebView, url: String, favicon: Bitmap?) {
                webview.visibility = View.INVISIBLE
            }

            override fun onPageFinished(view: WebView, url: String) {

                if (reportLoadError) {
                    loader.visibility = View.GONE
                    view.visibility = View.GONE
                    noData.visibility = View.VISIBLE
                    iconShare.visibility = View.GONE
                    btnTryAgain.setOnClickListener {
                        initWebView()
                    }
                } else {
                    view.visibility = View.VISIBLE
                    noData.visibility = View.GONE
                    iconShare.visibility = View.VISIBLE
                    loader.visibility = View.GONE
                }
                super.onPageFinished(view, url)

            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                reportLoadError = true

            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                view!!.loadUrl(request!!.url.toString())
                return false
            }

        }
        webview.settings.javaScriptEnabled = true
        webview.settings.domStorageEnabled = true
        webview.overScrollMode = WebView.OVER_SCROLL_NEVER

        webview.loadUrl("https://drive.google.com/viewerng/viewer?embedded=true&url=$pdfUrl")

    }
}

fun Context.toast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}