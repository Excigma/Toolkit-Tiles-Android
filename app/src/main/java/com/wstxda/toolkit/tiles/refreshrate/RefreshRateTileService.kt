package com.wstxda.toolkit.tiles.refreshrate

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import com.wstxda.toolkit.R
import com.wstxda.toolkit.activity.WriteSecureSettingsActivity
import com.wstxda.toolkit.base.BaseTileService
import com.wstxda.toolkit.manager.refreshrate.RefreshRateMode
import com.wstxda.toolkit.manager.refreshrate.RefreshRateModule
import kotlinx.coroutines.flow.Flow

class RefreshRateTileService : BaseTileService() {

    private val manager by lazy { RefreshRateModule.getInstance(applicationContext) }

    override fun onClick() {
        if (!manager.hasPermission()) {
            startActivityAndCollapse(WriteSecureSettingsActivity::class.java)
            return
        }
        manager.cycle()
        updateTile()
    }

    override fun flowsToCollect(): List<Flow<*>> = listOf(manager.mode)

    override fun updateTile() {
        val mode = manager.mode.value
        val hasPermission = manager.hasPermission()
        setTileState(
            state = if (mode != RefreshRateMode.OFF && hasPermission) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE,
            label = getString(R.string.refresh_rate_tile),
            subtitle = getString(
                when {
                    !hasPermission -> R.string.tile_setup
                    mode == RefreshRateMode.CAP_90 -> R.string.refresh_rate_90
                    else -> R.string.refresh_rate_120
                }
            ),
            icon = createIcon(mode),
        )
    }

    private fun createIcon(mode: RefreshRateMode): Icon {
        return try {
            val text = when (mode) {
                RefreshRateMode.CAP_90 -> "90"
                RefreshRateMode.OFF -> "120"
            }
            val size = 96
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFFFFFF.toInt()
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            // Draw circular refresh arrow faint background
            paint.alpha = 40
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 6f
            canvas.drawCircle(size / 2f, size / 2f, size / 2f - 8f, paint)
            paint.alpha = 255
            paint.style = Paint.Style.FILL
            // Draw text
            paint.textSize = 36f
            val y = size / 2f + 12f
            canvas.drawText(text, size / 2f, y, paint)
            // Small "Hz" 
            paint.textSize = 14f
            canvas.drawText("Hz", size / 2f, y + 16f, paint)
            Icon.createWithBitmap(bitmap)
        } catch (_: Exception) {
            Icon.createWithResource(this, R.drawable.ic_refresh_rate)
        }
    }
}
