package com.wstxda.toolkit.tiles.common

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.provider.Settings
import android.view.View
import android.view.WindowManager

class ForcePortraitDelegate(private val context: Context, prefsName: String) {

    private val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    fun isActive(): Boolean = prefs.getBoolean(KEY_ACTIVE, false)
    fun hasPermission(): Boolean = Settings.canDrawOverlays(context)

    fun setActive(enable: Boolean) {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (enable) {
            if (overlayView == null) {
                val view = View(context)
                val params = WindowManager.LayoutParams(
                    0, 0, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSPARENT
                )
                params.screenOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                try {
                    wm.addView(view, params)
                    overlayView = view
                    android.util.Log.d(TAG, "overlay added")
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "addView failed", e)
                }
            }
        } else {
            overlayView?.let {
                try { wm.removeView(it); android.util.Log.d(TAG, "overlay removed") }
                catch (e: Exception) { android.util.Log.e(TAG, "removeView failed", e) }
                overlayView = null
            }
        }
        prefs.edit().putBoolean(KEY_ACTIVE, enable).apply()
        try {
            Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
            Settings.System.putInt(context.contentResolver, Settings.System.USER_ROTATION, if (enable) 2 else 0)
        } catch (_: Exception) {}
    }

    fun sync() {
        val active = isActive()
        val hasPerm = hasPermission()
        if (active && overlayView == null && hasPerm) setActive(true)
        if (!active && overlayView != null) setActive(false)
    }

    fun permissionIntent(packageName: String): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    companion object {
        private const val KEY_ACTIVE = "active"
        private const val TAG = "ForcePortrait"
        private var overlayView: View? = null
    }
}
