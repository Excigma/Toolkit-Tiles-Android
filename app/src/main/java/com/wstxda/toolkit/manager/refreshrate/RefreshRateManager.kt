package com.wstxda.toolkit.manager.refreshrate

import android.content.Context
import android.provider.Settings
import com.wstxda.toolkit.base.SingletonHolder
import com.wstxda.toolkit.permissions.PermissionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RefreshRateMode(val peak: String, val labelRes: Int) {
    OFF("120.0", com.wstxda.toolkit.R.string.refresh_rate_120),
    CAP_90("90.0", com.wstxda.toolkit.R.string.refresh_rate_90);
}

class RefreshRateManager(context: Context) {

    private val appContext = context.applicationContext
    private val contentResolver = appContext.contentResolver
    private val permissionManager = PermissionManager(appContext)
    private val prefs = appContext.getSharedPreferences("refresh_rate_prefs", Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(getPersistedMode())
    val mode = _mode.asStateFlow()

    private fun getPersistedMode(): RefreshRateMode {
        return try {
            prefs.getString(KEY_PREF_MODE, null)?.let { RefreshRateMode.valueOf(it) } ?: RefreshRateMode.OFF
        } catch (_: Exception) { RefreshRateMode.OFF }
    }

    fun hasPermission(): Boolean = permissionManager.hasWriteSecureSettingsPermission()

    fun cycle() {
        if (!hasPermission()) return
        val next = if (_mode.value == RefreshRateMode.OFF) RefreshRateMode.CAP_90 else RefreshRateMode.OFF
        if (setMode(next)) {
            _mode.value = next
            prefs.edit().putString(KEY_PREF_MODE, next.name).commit()
        }
    }

    private fun setMode(mode: RefreshRateMode): Boolean {
        val peak = mode.peak
        try { Settings.Global.putString(contentResolver, KEY_PEAK, peak) } catch (_: Exception) {}
        try { Settings.Secure.putString(contentResolver, KEY_PEAK, peak) } catch (_: Exception) {}
        val v = if (mode == RefreshRateMode.OFF) "0" else "1"
        var secureOk = false
        try { secureOk = Settings.Secure.putString(contentResolver, KEY_OPLUS, v) } catch (_: Exception) {}
        // System write is best-effort (requires WRITE_SETTINGS); don't fail toggle if it throws
        try { Settings.System.putString(contentResolver, KEY_OPLUS, v) } catch (_: Exception) {}
        return secureOk
    }

    companion object {
        private const val KEY_PEAK = "peak_refresh_rate"
        private const val KEY_OPLUS = "oplus_customize_screen_refresh_rate"
        private const val KEY_PREF_MODE = "mode"
    }
}

object RefreshRateModule {
    private val holder = SingletonHolder(::RefreshRateManager)
    fun getInstance(context: Context) = holder.getInstance(context)
}
