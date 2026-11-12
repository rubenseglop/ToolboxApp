package com.ruben.utilidades

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** Botón opcional de linterna en el panel de ajustes rápidos de Android. */
class TorchTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()

        val toggle = {
            TorchController.toggle(this)
            refreshTile()
        }

        if (isLocked) {
            unlockAndRun { toggle() }
        } else {
            toggle()
        }
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val enabled = TorchController.isOn(this)
        tile.label = getString(R.string.torch_tile_label)
        tile.contentDescription = if (enabled) "Linterna encendida" else "Linterna apagada"
        tile.icon = Icon.createWithResource(this, R.drawable.ic_flashlight_tile)
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
