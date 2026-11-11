package com.ruben.utilidades

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Escáner de códigos QR y de barras con CameraX + ML Kit. */
@Composable
fun ScannerScreen() {
    CameraPermissionGate { ScannerContent() }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
private fun ScannerContent() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val clipboard = LocalClipboardManager.current
    val provider = rememberCameraProvider()

    val previewView = remember { PreviewView(context) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var cameraError by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var torchOn by remember { mutableStateOf(false) }

    // Evita procesar más fotogramas mientras se muestra un resultado.
    val paused = remember { AtomicBoolean(false) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            scanner.close()
            executor.shutdown()
        }
    }

    DisposableEffect(provider) {
        val p = provider
        if (p != null) {
            try {
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { proxy ->
                    val media = proxy.image
                    if (media == null || paused.get()) {
                        proxy.close()
                    } else {
                        val image = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { codes ->
                                val value = codes.firstNotNullOfOrNull { it.rawValue }
                                if (value != null && paused.compareAndSet(false, true)) {
                                    result = value
                                }
                            }
                            .addOnCompleteListener { proxy.close() }
                    }
                }

                p.unbindAll()
                camera = p.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
            } catch (e: Exception) {
                cameraError = true
            }
        }
        onDispose {
            p?.unbindAll()
            camera = null
        }
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

        // Marco guía
        Box(
            Modifier
                .align(Alignment.Center)
                .size(240.dp)
                .border(3.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
        )

        if (hasFlash) {
            FilledTonalIconToggleButton(
                checked = torchOn,
                onCheckedChange = { torchOn = it },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            ) {
                Icon(
                    if (torchOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    contentDescription = "Linterna",
                )
            }
        }

        val value = result
        if (value == null) {
            Text(
                "Apunta al código QR o de barras",
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp),
            )
        } else {
            ResultCard(
                value = value,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                onCopy = { clipboard.setText(AnnotatedString(value)) },
                onOpen = { openUrl(context, value) },
                onShare = { shareText(context, value) },
                onDismiss = {
                    result = null
                    paused.set(false)
                },
            )
        }
    }
}

@Composable
private fun ResultCard(
    value: String,
    modifier: Modifier,
    onCopy: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Código detectado", style = MaterialTheme.typography.labelLarge)
            SelectionContainer {
                Text(value, style = MaterialTheme.typography.bodyLarge, maxLines = 6)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCopy) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copiar")
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Filled.Share, contentDescription = "Compartir")
                }
                if (isWebUrl(value)) {
                    IconButton(onClick = onOpen) {
                        Icon(Icons.Filled.OpenInNew, contentDescription = "Abrir enlace")
                    }
                }
                Box(Modifier.weight(1f))
                if (isWebUrl(value)) {
                    OutlinedButton(onClick = onDismiss) { Text("Otro") }
                } else {
                    Button(onClick = onDismiss) { Text("Escanear otro") }
                }
            }
        }
    }
}

// Solo se ofrece "abrir" para http/https; nunca para esquemas arbitrarios (tel:, intent:, etc.).
private fun isWebUrl(value: String): Boolean =
    value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true)

private fun openUrl(context: Context, value: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(value)))
    } catch (_: ActivityNotFoundException) {
        // No hay navegador instalado.
    }
}

private fun shareText(context: Context, value: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, value)
    }
    context.startActivity(Intent.createChooser(send, null))
}
