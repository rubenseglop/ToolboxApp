package com.ruben.utilidades

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** Widget de un solo toque: alterna la linterna sin abrir ToolboxApp. */
class TorchWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) {
            TorchController.toggle(context)
            updateAll(context)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
    }

    companion object {
        private const val ACTION_TOGGLE = "com.ruben.utilidades.action.TOGGLE_TORCH"

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, TorchWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { updateWidget(context, manager, it) }
        }

        private fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val enabled = TorchController.isOn(context)
            val views = RemoteViews(context.packageName, R.layout.widget_torch)
            views.setImageViewResource(
                R.id.torch_widget_icon,
                if (enabled) R.drawable.ic_flashlight_on else R.drawable.ic_flashlight_off
            )
            views.setTextViewText(
                R.id.torch_widget_label,
                if (enabled) "APAGAR" else "ENCENDER"
            )

            val intent = Intent(context, TorchWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.torch_widget_button, pendingIntent)
            manager.updateAppWidget(appWidgetId, views)
        }
    }
}
