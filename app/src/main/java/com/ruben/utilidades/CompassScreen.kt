package com.ruben.utilidades

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val POINTS = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")

/** Brújula basada en el sensor de vector de rotación (fusiona magnetómetro, acelerómetro y giroscopio). */
@Composable
fun CompassScreen() {
    val context = LocalContext.current
    val sm = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensor = remember { sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }

    if (sensor == null) {
        CenteredMessage("Este dispositivo no tiene los sensores necesarios para la brújula.")
        return
    }

    var azimuth by remember { mutableFloatStateOf(0f) }
    var accuracy by remember { mutableIntStateOf(SensorManager.SENSOR_STATUS_ACCURACY_HIGH) }

    DisposableEffect(sensor) {
        val rotation = FloatArray(9)
        val orientation = FloatArray(3)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                SensorManager.getOrientation(rotation, orientation)
                val target = (Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f

                // Suavizado respetando el salto 359° -> 0°.
                var diff = target - azimuth
                if (diff > 180f) diff -= 360f
                if (diff < -180f) diff += 360f
                azimuth = (azimuth + diff * 0.15f + 360f) % 360f
            }

            override fun onAccuracyChanged(sensor: Sensor, newAccuracy: Int) {
                accuracy = newAccuracy
            }
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener) }
    }

    val degrees = azimuth.roundToInt() % 360
    val point = POINTS[((azimuth + 22.5f) / 45f).toInt() % 8]

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$degrees° $point", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(24.dp))
        CompassDial(azimuth)
        Spacer(Modifier.height(24.dp))
        Text(
            text = if (accuracy < SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM)
                "Precisión baja: mueve el móvil dibujando un 8 en el aire para calibrar."
            else
                "Mantén el móvil en horizontal.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CompassDial(azimuth: Float) {
    val ink = MaterialTheme.colorScheme.onSurface
    val faint = MaterialTheme.colorScheme.outline
    val north = Color(0xFFD32F2F)

    Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
        // Esfera: gira en sentido contrario al rumbo para que el norte señale al norte real.
        Canvas(Modifier.fillMaxSize().rotate(-azimuth)) {
            val c = center
            val r = size.minDimension / 2f - 4.dp.toPx()
            drawCircle(faint, radius = r, style = Stroke(width = 2.dp.toPx()))

            for (i in 0 until 72) {
                val major = i % 9 == 0 // cada 45°
                val len = if (major) 16.dp.toPx() else 8.dp.toPx()
                rotate(i * 5f, pivot = c) {
                    drawLine(
                        color = if (i == 0) north else if (major) ink else faint,
                        start = Offset(c.x, c.y - r),
                        end = Offset(c.x, c.y - r + len),
                        strokeWidth = if (major) 3.dp.toPx() else 1.5.dp.toPx(),
                    )
                }
            }

            val paint = Paint().apply {
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                textSize = 28.dp.toPx()
                typeface = Typeface.DEFAULT_BOLD
            }
            val letterR = r - 40.dp.toPx()
            listOf("N" to 0f, "E" to 90f, "S" to 180f, "O" to 270f).forEach { (label, deg) ->
                val rad = Math.toRadians(deg.toDouble())
                val x = c.x + letterR * sin(rad).toFloat()
                val y = c.y - letterR * cos(rad).toFloat() + paint.textSize / 3f
                paint.color = (if (label == "N") north else ink).toArgb()
                drawContext.canvas.nativeCanvas.drawText(label, x, y, paint)
            }
        }

        // Marca fija: indica hacia dónde apunta la parte superior del móvil.
        Canvas(Modifier.fillMaxSize()) {
            val path = Path().apply {
                moveTo(center.x, -2.dp.toPx())
                lineTo(center.x - 12.dp.toPx(), 22.dp.toPx())
                lineTo(center.x + 12.dp.toPx(), 22.dp.toPx())
                close()
            }
            drawPath(path, north)
        }
    }
}
