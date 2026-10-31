package ai.moeru.airi_pocket

  import android.content.Intent
  import android.net.Uri
  import android.os.Build
  import android.provider.Settings
  import com.getcapacitor.JSObject
  import com.getcapacitor.Plugin
  import com.getcapacitor.PluginCall
  import com.getcapacitor.PluginMethod
  import com.getcapacitor.annotation.CapacitorPlugin

  @CapacitorPlugin(name = "Overlay")
  class OverlayPlugin : Plugin() {

      @PluginMethod
      fun checkPermission(call: PluginCall) {
          val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
              Settings.canDrawOverlays(context)
          else
              true
          call.resolve(JSObject().put("granted", granted))
      }

      @PluginMethod
      fun requestPermission(call: PluginCall) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
  !Settings.canDrawOverlays(context)) {
              activity.startActivity(
                  Intent(
                      Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                      Uri.parse("package:${context.packageName}")
                  )
              )
          }
          call.resolve()
      }

      @PluginMethod
      fun startOverlay(call: PluginCall) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
  !Settings.canDrawOverlays(context)) {
              call.reject("NO_PERMISSION", "Нет разрешения. Вызови requestPermission()
  сначала.")
              return
          }
          val url = call.getString("url") ?: "https://airi.moeru.ai"
          val intent = Intent(context, OverlayService::class.java).apply {
              putExtra(OverlayService.EXTRA_URL, url)
          }
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
              context.startForegroundService(intent)
          else
              context.startService(intent)
          call.resolve()
      }

      @PluginMethod
      fun stopOverlay(call: PluginCall) {
          context.stopService(Intent(context, OverlayService::class.java))
          call.resolve()
      }
  }
