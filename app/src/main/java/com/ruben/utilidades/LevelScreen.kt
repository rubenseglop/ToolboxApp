package com.ruben.utilidades

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.asin
import kotlin.math.hypot
import kotlin.math.roundToInt

private const val G = SensorManager.GRAVITY_EARTH

/** Nivel de burbuja con el acelerómetro. Colocar el móvil plano sobre la superficie. */
@Composable
fun LevelScreen() {
    val context = LocalContext.current
    val sm = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensor = remember { sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    if (sensor == null) {
        CenteredMessage("Este dispositivo no tiene acelerómetro.")
        return
    }

    var ax by remember { mutableFloatStateOf(0f) }
    var ay by remember { mutableFloatStateOf(0f) }

    DisposableEffect(sensor) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // Filtro paso bajo para que la burbuja no tiemble.
                ax = ax * 0.85f + event.values[0] * 0.15f
                ay = ay * 0.85f + event.values[1] * 0.15f
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(listener) }
    }

    val tiltX = Math.toDegrees(asin((ax / G).coerceIn(-1f, 1f)).toDouble()).toFloat()
    val tiltY = Math.toDegrees(asin((ay / G).coerceIn(-1f, 1f)).toDouble()).toFloat()
    val isLevel = hypot(tiltX, tiltY) < 1f

    val good = Color(0xFF2E7D32)
    val ink = MaterialTheme.colorScheme.onSurface
    val faint = MaterialTheme.colorScheme.outline
    val bubbleColor = if (isLevel) good else MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (isLevel) "Nivelado" else "${hypot(tiltX, tiltY).roundToInt()}°",
            style = MaterialTheme.typography.displayMedium,
            color = if (isLevel) good else ink,
        )
        Spacer(Modifier.height(24.dp))

        Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
            val c = center
            val ringR = size.minDimension / 2f - 4.dp.toPx()
            val bubbleR = 24.dp.toPx()
            val maxTravel = ringR - bubbleR

            drawCircle(faint, radius = ringR, style = Stroke(2.dp.toPx()))
            drawCircle(faint, radius = ringR / 2f, style = Stroke(1.dp.toPx()))
            drawLine(faint, Offset(c.x - ringR, c.y), Offset(c.x + ringR, c.y), 1.dp.toPx())
            drawLine(faint, Offset(c.x, c.y - ringR), Offset(c.x, c.y + ringR), 1.dp.toPx())
            drawCircle(ink, radius = bubbleR, style = Stroke(2.dp.toPx()))

            // La burbuja sube hacia el lado más alto. Deflexión máxima a ~15° de inclinación.
            val full = G * 0.26f
            var nx = ax / full
            var ny = -ay / full
            val len = hypot(nx, ny)
            if (len > 1f) {
                nx /= len
                ny /= len
            }
            drawCircle(
                color = bubbleColor.copy(alpha = 0.85f),
                radius = bubbleR,
                center = Offset(c.x + nx * maxTravel, c.y + ny * maxTravel),
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "X: ${"%.1f".format(tiltX)}°    Y: ${"%.1f".format(tiltY)}°",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text("Coloca el móvil plano sobre la superficie.", style = MaterialTheme.typography.bodyMedium)
    }
}
