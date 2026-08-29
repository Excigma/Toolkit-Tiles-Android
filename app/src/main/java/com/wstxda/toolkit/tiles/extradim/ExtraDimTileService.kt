package com.wstxda.toolkit.tiles.extradim

import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.provider.Settings
import android.service.quicksettings.Tile
import com.wstxda.toolkit.R
import com.wstxda.toolkit.activity.WriteSecureSettingsActivity
import com.wstxda.toolkit.base.BaseTileService

/**
 * Extra Dim - restores AOSP Extra Dim removed on Oppo/ColorOS.
 * Toggles Settings.Secure.reduce_bright_colors_activated (0/1).
 * Level left as-is. Requires WRITE_SECURE_SETTINGS via ADB.
 */
class ExtraDimTileService : BaseTileService() {

    companion object {
        private const val KEY_ACTIVATED = "reduce_bright_colors_activated"
        private var cachedEnabled: Boolean? = null
        private var cachedAt: Long = 0
    }

    private fun hasPermission(): Boolean {
        return checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    }

    private fun isEnabled(): Boolean {
        val now = System.currentTimeMillis()
        if (cachedEnabled != null && now - cachedAt < 2000) {
            return cachedEnabled!!
        }

        // Secure key is @hide, so we're not able to read the value. Try to guess from prefs if we can't read it.
        var v = try {
            Settings.Secure.getInt(contentResolver, KEY_ACTIVATED, -1)
        } catch (_: Exception) { -1 }

        if (v == 1) return true
        if (v == 0) return try {
            // If Settings readable and says 0, return this
            val prefs = getSharedPreferences("extra_dim_prefs", MODE_PRIVATE)
            val p = prefs.getBoolean("enabled", false)

            // If prefs says enabled but Settings says 0, trust prefs
            if (p && cachedEnabled == null) true else false
        } catch (_: Exception) { false }

        // Not readable, fall back to prefs
        return try {
            getSharedPreferences("extra_dim_prefs", MODE_PRIVATE).getBoolean("enabled", false)
        } catch (_: Exception) { false }
    }

    private fun setEnabled(enabled: Boolean): Boolean {
        cachedEnabled = enabled
        cachedAt = System.currentTimeMillis()
        try { getSharedPreferences("extra_dim_prefs", MODE_PRIVATE).edit().putBoolean("enabled", enabled).commit() } catch (_: Exception) {}
        return try {
            Settings.Secure.putInt(contentResolver, KEY_ACTIVATED, if (enabled) 1 else 0)
        } catch (e: Exception) {
            android.util.Log.e("ExtraDimTile", "setEnabled failed", e)
            false
        }
    }

    override fun onClick() {
        if (!hasPermission()) {
            startActivityAndCollapse(WriteSecureSettingsActivity::class.java)
            return
        }
        val newEnabled = !isEnabled()
        val ok = setEnabled(newEnabled)
        android.util.Log.d("ExtraDimTile", "onClick newEnabled=$newEnabled ok=$ok verify=${isEnabled()}")

        setTileState(
            state = if (newEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE,
            label = getString(R.string.extra_dim_tile),
            subtitle = getString(if (newEnabled) R.string.tile_on else R.string.tile_off),
            icon = Icon.createWithResource(this, R.drawable.ic_extra_dim),
        )
    }

    override fun updateTile() {
        val enabled = isEnabled()
        val hasPermission = hasPermission()

        setTileState(
            state = if (enabled && hasPermission) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE,
            label = getString(R.string.extra_dim_tile),
            subtitle = getString(if (enabled && hasPermission) R.string.tile_on else R.string.tile_off),
            icon = Icon.createWithResource(this, R.drawable.ic_extra_dim),
        )
    }
}
