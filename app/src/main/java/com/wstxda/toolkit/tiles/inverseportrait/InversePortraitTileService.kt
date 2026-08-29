package com.wstxda.toolkit.tiles.inverseportrait

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import com.wstxda.toolkit.R
import com.wstxda.toolkit.base.BaseTileService
import com.wstxda.toolkit.tiles.common.ForcePortraitDelegate

class InversePortraitTileService : BaseTileService() {

    private val delegate by lazy { ForcePortraitDelegate(this, "inverse_portrait_prefs") }

    override fun onClick() {
        if (!delegate.hasPermission()) {
            try {
                val dialog = android.app.AlertDialog.Builder(this)
                    .setTitle(getString(R.string.overlay_permission_title))
                    .setMessage(getString(R.string.overlay_permission_message))
                    .setNegativeButton(android.R.string.cancel) { d, _ -> d.dismiss() }
                    .setPositiveButton(getString(R.string.tile_setup)) { _, _ ->
                        startActivityAndCollapse(delegate.permissionIntent(packageName))
                    }.create()
                showDialog(dialog)
            } catch (_: Exception) {
                startActivityAndCollapse(delegate.permissionIntent(packageName))
            }
            return
        }
        val newActive = !delegate.isActive()
        delegate.setActive(newActive)
        setTileState(
            state = if (newActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE,
            label = getString(R.string.inverse_portrait_tile),
            subtitle = getString(if (newActive) R.string.tile_on else R.string.tile_off),
            icon = Icon.createWithResource(this, if (newActive) R.drawable.ic_inverse_portrait else R.drawable.ic_inverse_portrait_off),
        )
    }

    override fun updateTile() {
        delegate.sync()
        val active = delegate.isActive()
        val hasPerm = delegate.hasPermission()
        setTileState(
            state = if (active && hasPerm) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE,
            label = getString(R.string.inverse_portrait_tile),
            subtitle = getString(when {
                !hasPerm -> R.string.tile_setup
                active -> R.string.tile_on
                else -> R.string.tile_off
            }),
            icon = Icon.createWithResource(this, if (active) R.drawable.ic_inverse_portrait else R.drawable.ic_inverse_portrait_off),
        )
    }
}
