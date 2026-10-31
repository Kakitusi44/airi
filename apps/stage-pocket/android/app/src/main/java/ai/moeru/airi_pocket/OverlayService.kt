package ai.moeru.airi_pocket

  import android.app.Notification
  import android.app.NotificationChannel
  import android.app.NotificationManager
  import android.app.PendingIntent
  import android.app.Service
  import android.content.Context
  import android.content.Intent
  import android.graphics.Color
  import android.graphics.PixelFormat
  import android.os.Build
  import android.os.IBinder
  import android.view.Gravity
  import android.view.MotionEvent
  import android.view.View
  import android.view.WindowManager
  import android.webkit.WebSettings
  import android.webkit.WebView
  import android.webkit.WebViewClient
  import android.widget.FrameLayout
  import androidx.core.app.NotificationCompat

  class OverlayService : Service() {
      private var windowManager: WindowManager? = null
      private var overlayContainer: View? = null

      companion object {
          const val CHANNEL_ID = "airi_overlay_channel"
          const val NOTIFICATION_ID = 1001
          const val EXTRA_URL = "overlay_url"
          const val ACTION_STOP = "ai.moeru.airi_pocket.STOP_OVERLAY"
      }

      override fun onBind(intent: Intent?): IBinder? = null

      override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
          if (intent?.action == ACTION_STOP) {
              stopSelf()
              return START_NOT_STICKY
          }
          if (overlayContainer == null) {
              val url = intent?.getStringExtra(EXTRA_URL) ?: "https://airi.moeru.ai"
              showOverlay(url)
          }
          return START_STICKY
      }

      override fun onCreate() {
          super.onCreate()
          createNotificationChannel()
          startForeground(NOTIFICATION_ID, buildNotification())
      }

      private fun showOverlay(url: String) {
          windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

          val params = WindowManager.LayoutParams(
              dpToPx(220),
              dpToPx(300),
              WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
              WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
              PixelFormat.TRANSLUCENT
          ).apply {
              gravity = Gravity.TOP or Gravity.END
              x = dpToPx(8)
              y = dpToPx(120)
          }

          val container = FrameLayout(this)

          val webView = WebView(this).apply {
              settings.apply {
                  javaScriptEnabled = true
                  domStorageEnabled = true
                  mediaPlaybackRequiresUserGesture = false
                  cacheMode = WebSettings.LOAD_DEFAULT
              }
              setBackgroundColor(Color.TRANSPARENT)
              webViewClient = WebViewClient()
              loadUrl(url)
          }

          // Semi-transparent strip at top — drag it to move the overlay window
          val dragHandle = View(this).apply {
              setBackgroundColor(Color.argb(80, 0, 0, 0))
          }

          container.addView(webView, FrameLayout.LayoutParams(
              FrameLayout.LayoutParams.MATCH_PARENT,
              FrameLayout.LayoutParams.MATCH_PARENT
          ))
          container.addView(dragHandle, FrameLayout.LayoutParams(
              FrameLayout.LayoutParams.MATCH_PARENT,
              dpToPx(22)
          ).apply { gravity = Gravity.TOP })

          var initX = 0; var initY = 0
          var touchX = 0f; var touchY = 0f

          dragHandle.setOnTouchListener { _, event ->
              when (event.action) {
                  MotionEvent.ACTION_DOWN -> {
                      initX = params.x; initY = params.y
                      touchX = event.rawX; touchY = event.rawY
                      true
                  }
                  MotionEvent.ACTION_MOVE -> {
                      params.x = initX + (event.rawX - touchX).toInt()
                      params.y = initY + (event.rawY - touchY).toInt()
                      windowManager?.updateViewLayout(container, params)
                      true
                  }
                  else -> false
              }
          }

          overlayContainer = container
          windowManager!!.addView(container, params)
      }

      override fun onDestroy() {
          overlayContainer?.let { windowManager?.removeView(it) }
          overlayContainer = null
          super.onDestroy()
      }

      private fun createNotificationChannel() {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              val channel = NotificationChannel(
                  CHANNEL_ID, "AIRI Overlay", NotificationManager.IMPORTANCE_LOW
              ).apply { description = "AIRI character floating overlay" }

  getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
          }
      }

      private fun buildNotification(): Notification {
          val stopIntent = PendingIntent.getService(
              this, 0,
              Intent(this, OverlayService::class.java).apply { action = ACTION_STOP },
              PendingIntent.FLAG_IMMUTABLE
          )
          return NotificationCompat.Builder(this, CHANNEL_ID)
              .setContentTitle("AIRI is active")
              .setContentText("Character is displayed over other apps")
              .setSmallIcon(android.R.drawable.ic_dialog_info)
              .setPriority(NotificationCompat.PRIORITY_LOW)
              .setOngoing(true)
              .addAction(0, "Hide", stopIntent)
              .build()
      }

      private fun dpToPx(dp: Int): Int =
          (dp * resources.displayMetrics.density).toInt()
  }
