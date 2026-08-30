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

    // Try forcing rotation with settings write and overlay permissions.
    fun hasWriteSettingsPermission(): Boolean = Settings.System.canWrite(context)
    fun hasOverlayPermission(): Boolean = Settings.canDrawOverlays(context)
    fun hasWriteSecureSettingsPermission(): Boolean =
        context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == android.content.pm.PackageManager.PERMISSION_GRANTED
    fun hasPermission(): Boolean = hasWriteSettingsPermission() && hasOverlayPermission()
    fun hasAnyPermission(): Boolean = hasWriteSettingsPermission() || hasOverlayPermission()

    fun setActive(enable: Boolean) {
        prefs.edit().putBoolean(KEY_ACTIVE, enable).apply()

        var rotationOk = false
        if (hasWriteSettingsPermission()) {
            try {
                Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
                Settings.System.putInt(context.contentResolver, Settings.System.USER_ROTATION, if (enable) 2 else 0)
                rotationOk = true
                android.util.Log.d(TAG, "USER_ROTATION set to ${if (enable) 2 else 0}")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "put USER_ROTATION failed", e)
            }
        }
        if (hasWriteSecureSettingsPermission()) {
            try {
                Settings.Secure.putInt(context.contentResolver, "ignore_orientation_request", if (enable) 1 else 0)
                android.util.Log.d(TAG, "ignore_orientation_request set to ${if (enable) 1 else 0}")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "put ignore_orientation_request failed", e)
            }
            try {
                Settings.Secure.putInt(context.contentResolver, "fixed_to_user_rotation", if (enable) 1 else 0)
            } catch (_: Exception) {}
        }

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (enable) {
            if (overlayView == null && hasOverlayPermission()) {
                val view = View(context)
                val params = WindowManager.LayoutParams(
                    0, 0, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                    PixelFormat.TRANSPARENT
                )
                params.screenOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                try {
                    wm.addView(view, params)
                    overlayView = view
                    android.util.Log.d(TAG, "overlay added (rotationOk=$rotationOk)")
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "addView failed", e)
                }
            } else if (overlayView == null) {
                android.util.Log.d(TAG, "overlay skipped - no permission, using USER_ROTATION only")
            }
        } else {
            overlayView?.let {
                try { wm.removeView(it); android.util.Log.d(TAG, "overlay removed") }
                catch (e: Exception) { android.util.Log.e(TAG, "removeView failed", e) }
                overlayView = null
            }
        }
    }

    fun sync() {
        val active = isActive()
        if (active && overlayView == null && hasOverlayPermission()) setActive(true)
        if (!active && overlayView != null) setActive(false)
        if (active && hasWriteSettingsPermission()) {
            try {
                val cur = Settings.System.getInt(context.contentResolver, Settings.System.USER_ROTATION, -1)
                val accel = Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, -1)
                if (cur != 2 || accel != 0) setActive(true)
            } catch (_: Exception) {}
        }
        if (active && hasWriteSecureSettingsPermission()) {
            try {
                val ign = Settings.Secure.getInt(context.contentResolver, "ignore_orientation_request", -1)
                if (ign != 1) setActive(true)
            } catch (_: Exception) {}
        }
    }

    fun writeSettingsIntent(packageName: String): Intent =
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
