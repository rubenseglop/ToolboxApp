package com.ruben.utilidades

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.service.quicksettings.TileService

/**
 * Estado compartido de la linterna para la pantalla de la app, el widget y el botón rápido.
 * El flash no requiere permiso CAMERA cuando se controla mediante setTorchMode().
 */
object TorchController {
    private const val PREFS = "torch_widget_state"
    private const val KEY_IS_ON = "is_on"

    fun isOn(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_IS_ON, false)

    fun toggle(context: Context): Boolean {
        val target = !isOn(context)
        return setTorch(context, target)
    }

    fun setTorch(context: Context, enabled: Boolean): Boolean {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            recordState(context, false)
            return false
        }

        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = runCatching {
            manager.cameraIdList.firstOrNull { id ->
                val characteristics = manager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                    characteristics.get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_BACK
            }
        }.getOrNull() ?: return false

        return try {
            manager.setTorchMode(cameraId, enabled)
            recordState(context, enabled)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun recordState(context: Context, enabled: Boolean, notifyOtherControls: Boolean = true) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IS_ON, enabled)
            .apply()

        if (!notifyOtherControls) return

        TorchWidgetProvider.updateAll(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching {
                TileService.requestListeningState(
                    context,
                    ComponentName(context, TorchTileService::class.java)
                )
            }
        }
    }
}
