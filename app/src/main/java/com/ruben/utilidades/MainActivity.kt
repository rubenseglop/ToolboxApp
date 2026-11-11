package com.ruben.utilidades

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UtilidadesTheme {
                AppRoot()
            }
        }
    }
}

private enum class Tool(val label: String, val icon: ImageVector) {
    Lupa("Lupa", Icons.Filled.ZoomIn),
    Escaner("Escáner QR", Icons.Filled.QrCodeScanner),
    Linterna("Linterna", Icons.Filled.FlashlightOn),
    Brujula("Brújula", Icons.Filled.Explore),
    Nivel("Nivel", Icons.Filled.Straighten),
}

@Composable
private fun AppRoot() {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val tools = Tool.entries

    Scaffold(
        bottomBar = {
            NavigationBar {
                tools.forEachIndexed { index, tool ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(tool.icon, contentDescription = tool.label) },
                        label = { Text(tool.label, maxLines = 1) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            // Solo se compone la herramienta activa: así la cámara y los sensores
            // se liberan automáticamente al cambiar de pestaña.
            when (tools[selected]) {
                Tool.Lupa -> MagnifierScreen()
                Tool.Escaner -> ScannerScreen()
                Tool.Linterna -> TorchScreen()
                Tool.Brujula -> CompassScreen()
                Tool.Nivel -> LevelScreen()
            }
        }
    }
}

@Composable
private fun UtilidadesTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colors, content = content)
}
