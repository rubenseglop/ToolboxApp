package com.ruben.utilidades

import android.graphics.Bitmap
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.pow

/** Lupa: vista previa de la cámara con zoom, linterna y congelar imagen. */
@Composable
fun MagnifierScreen() {
    CameraPermissionGate { MagnifierContent() }
}

@Composable
private fun MagnifierContent() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val provider = rememberCameraProvider()

    val previewView = remember {
        PreviewView(context).apply {
            // COMPATIBLE (TextureView) permite obtener un bitmap para "congelar".
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var cameraError by remember { mutableStateOf(false) }
    var minZoom by remember { mutableFloatStateOf(1f) }
    var maxZoom by remember { mutableFloatStateOf(1f) }
    var zoomFraction by remember { mutableFloatStateOf(0.25f) }
    var torchOn by remember { mutableStateOf(false) }
    var frozen by remember { mutableStateOf<Bitmap?>(null) }

    DisposableEffect(provider) {
        val p = provider
        if (p != null) {
            try {
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)
                p.unbindAll()
                val cam = p.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                )
                cam.cameraInfo.zoomState.value?.let {
                    minZoom = it.minZoomRatio
                    maxZoom = minOf(it.maxZoomRatio, 10f)
                }
                camera = cam
            } catch (e: Exception) {
                cameraError = true
            }
        }
        onDispose {
            p?.unbindAll()
            camera = null
        }
    }

    // Zoom exponencial: se siente más natural que uno lineal.
    LaunchedEffect(camera, zoomFraction, minZoom, maxZoom) {
        val ratio = minZoom * (maxZoom / minZoom).pow(zoomFraction)
        camera?.cameraControl?.setZoomRatio(ratio)
    }

    val hasFlash = camera?.cameraInfo?.hasFlashUnit() == true
    LaunchedEffect(camera, torchOn) {
        if (hasFlash) camera?.cameraControl?.enableTorch(torchOn)
    }

    if (cameraError) {
        CenteredMessage("No se ha podido abrir la cámara trasera.")
        return
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        frozen?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Imagen congelada",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { zoomFraction = (zoomFraction - 0.1f).coerceAtLeast(0f) },
                    enabled = frozen == null,
                ) { Icon(Icons.Filled.Remove, contentDescription = "Alejar", tint = Color.White) }

                Slider(
                    value = zoomFraction,
                    onValueChange = { zoomFraction = it },
                    enabled = frozen == null,
                    modifier = Modifier.weight(1f),
                )

                IconButton(
                    onClick = { zoomFraction = (zoomFraction + 0.1f).coerceAtMost(1f) },
                    enabled = frozen == null,
                ) { Icon(Icons.Filled.Add, contentDescription = "Acercar", tint = Color.White) }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                if (hasFlash) {
                    FilledTonalButton(onClick = { torchOn = !torchOn }) {
                        Icon(
                            if (torchOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                            contentDescription = null,
                        )
                        Text(if (torchOn) "  Luz encendida" else "  Luz apagada")
                    }
                }
                FilledTonalButton(onClick = {
                    frozen = if (frozen == null) previewView.bitmap else null
                }) {
                    Icon(
                        if (frozen == null) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                    )
                    Text(if (frozen == null) "  Congelar" else "  Reanudar")
                }
            }
        }
    }
}
