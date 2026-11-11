package com.ruben.utilidades

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Linterna con el flash del móvil, más modo SOS. No necesita permiso de cámara. */
@Composable
fun TorchScreen() {
    val context = LocalContext.current
    val hasFlash = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }
    if (hasFlash) TorchContent() else CenteredMessage("Este dispositivo no tiene flash.")
}

// Duraciones en ms; posiciones pares = encendido, impares = apagado. Código Morse de "SOS".
private val SOS_PATTERN = longArrayOf(
    250, 250, 250, 250, 250, 750, // S
    750, 250, 750, 250, 750, 750, // O
    250, 250, 250, 250, 250, 1750, // S + pausa antes de repetir
)

@Composable
private fun TorchContent() {
    val context = LocalContext.current
    val cm = remember { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    val cameraId = remember {
        runCatching {
            cm.cameraIdList.firstOrNull { id ->
                val c = cm.getCameraCharacteristics(id)
                c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                    c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            }
        }.getOrNull()
    }

    if (cameraId == null) {
        CenteredMessage("No se ha encontrado el flash de la cámara trasera.")
        return
    }

    var isOn by remember { mutableStateOf(false) }
    var available by remember { mutableStateOf(true) }
    var sos by remember { mutableStateOf(false) }

    fun setTorch(on: Boolean) {
        try {
            cm.setTorchMode(cameraId, on)
        } catch (_: Exception) {
            // Cámara en uso por otra app, etc. El callback actualizará el estado real.
        }
    }

    // Refleja el estado real del flash (por ejemplo si otra app lo apaga).
    DisposableEffect(cameraId) {
        val callback = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(id: String, enabled: Boolean) {
                if (id == cameraId) {
                    isOn = enabled
                    available = true
                }
            }

            override fun onTorchModeUnavailable(id: String) {
                if (id == cameraId) available = false
            }
        }
        cm.registerTorchCallback(callback, null)
        onDispose { cm.unregisterTorchCallback(callback) }
    }

    LaunchedEffect(sos) {
        if (sos) {
            try {
                while (true) {
                    SOS_PATTERN.forEachIndexed { i, ms ->
                        setTorch(i % 2 == 0)
                        delay(ms)
                    }
                }
            } finally {
                setTorch(false)
            }
        }
    }

    val active = isOn || sos

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(
                    if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .clickable(enabled = available) {
                    if (sos) {
                        sos = false
                    } else {
                        setTorch(!isOn)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (active) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                contentDescription = if (active) "Apagar linterna" else "Encender linterna",
                modifier = Modifier.size(96.dp),
                tint = if (active) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = when {
                !available -> "Flash no disponible (cámara en uso)"
                sos -> "SOS"
                isOn -> "Encendida"
                else -> "Apagada"
            },
            style = MaterialTheme.typography.headlineSmall,
        )

        Spacer(Modifier.height(16.dp))

        FilterChip(
            selected = sos,
            onClick = { sos = !sos },
            enabled = available,
            label = { Text("Modo SOS") },
        )
    }
}
