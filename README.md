# Utilidades

Aplicación Android sencilla que reúne herramientas prácticas y accesos directos pensados para utilizarse con facilidad.

## Funciones

- **Linterna:** activa y desactiva el flash del teléfono desde la aplicación o desde el widget de la pantalla de inicio.
- **Widget de linterna:** botón grande de un solo toque; cambia de aspecto según esté encendida o apagada.
- **Acceso rápido de linterna:** mosaico opcional para el panel de Ajustes rápidos de Android.
- **Escáner QR y códigos de barras:** abre directamente el lector desde la aplicación o desde su widget.
- **Lupa, brújula y nivel:** herramientas adicionales incluidas en la aplicación.

## Requisitos

- Android 8.0 (API 26) o posterior.
- Cámara con flash para utilizar la linterna.
- Cámara para escanear códigos QR y de barras.

La aplicación solicita el permiso de cámara cuando es necesario. El escáner utiliza Google ML Kit con el modelo incluido en la aplicación.

## Instalación

1. Abre la sección [Releases](https://github.com/rubenseglop/ToolboxApp/releases).
2. Descarga el APK adjunto a la versión más reciente.
3. Instálalo en el teléfono. Android puede pedir autorización para instalar aplicaciones desde el navegador o gestor de archivos.
4. Abre **Utilidades** una vez para completar la inicialización y conceder los permisos que solicite.

### Añadir los widgets

- Mantén pulsada una zona vacía de la pantalla de inicio.
- Entra en **Widgets**.
- Busca **Utilidades**.
- Añade **Linterna** o **Escáner QR** a la pantalla de inicio.

El widget de linterna alterna el flash con cada toque. El widget del escáner abre directamente el lector.

### Añadir el acceso rápido de linterna

Abre el panel de Ajustes rápidos, pulsa editar y arrastra el mosaico **Linterna** a la zona activa.

## Compilar desde el código fuente

El proyecto utiliza Kotlin, Jetpack Compose y Gradle.

Desde la raíz del repositorio:

```bash
gradle assembleDebug
```

El APK de depuración se genera en:

```
app/build/outputs/apk/debug/app-debug.apk
```

También hay un workflow de GitHub Actions que compila el APK y publica el resultado como artefacto de la ejecución.

## Compatibilidad

- `minSdk 26` (Android 8.0).
- `targetSdk 35`.

## Repositorio

[github.com/rubenseglop/ToolboxApp](https://github.com/rubenseglop/ToolboxApp)
