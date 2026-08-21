package com.gesys.linkbuds_widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LinkBudsWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildViews(context, appWidgetManager, id))
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        // Fires live as the user resizes the widget, so it can switch between the compact and
        // ANC-expanded layouts without waiting for the next data refresh.
        appWidgetManager.updateAppWidget(appWidgetId, buildViews(context, appWidgetManager, appWidgetId))
    }

    companion object {
        private const val COLOR_CONNECTED = 0xFF4CAF50.toInt()
        private const val COLOR_DISCONNECTED = 0xFF9E9E9E.toInt()
        private const val BG_WIDTH_PX = 400
        private const val BG_HEIGHT_PX = 200

        // Android's cell-size formula is roughly (cells * 70dp) - 30dp, so 2 cells ~= 110dp.
        private const val EXPANDED_HEIGHT_THRESHOLD_DP = 110

        private const val ANC_REQUEST_CODE_OFF = 101
        private const val ANC_REQUEST_CODE_NC = 102
        private const val ANC_REQUEST_CODE_AMBIENT = 103

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, LinkBudsWidgetProvider::class.java))
            for (id in ids) {
                manager.updateAppWidget(id, buildViews(context, manager, id))
            }
        }

        private fun percentText(value: Int?) = if (value != null) "$value%" else "—"

        private fun buildBackground(context: Context): Bitmap {
            val density = context.resources.displayMetrics.density
            val color = WidgetPrefs.color(context)
            val opacityPercent = WidgetPrefs.opacityPercent(context).coerceIn(0, 100)
            val cornerRadiusDp = WidgetPrefs.cornerRadiusDp(context).coerceIn(0, 48)

            val bitmap = Bitmap.createBitmap(BG_WIDTH_PX, BG_HEIGHT_PX, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                this.alpha = opacityPercent * 255 / 100
            }
            val radiusPx = cornerRadiusDp * density
            canvas.drawRoundRect(
                RectF(0f, 0f, BG_WIDTH_PX.toFloat(), BG_HEIGHT_PX.toFloat()),
                radiusPx,
                radiusPx,
                paint
            )
            return bitmap
        }

        private fun isExpanded(appWidgetManager: AppWidgetManager, appWidgetId: Int): Boolean {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            return heightDp >= EXPANDED_HEIGHT_THRESHOLD_DP
        }

        private fun buildViews(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int): RemoteViews {
            val expanded = isExpanded(appWidgetManager, appWidgetId)
            val layoutRes = if (expanded) R.layout.linkbuds_widget_expanded else R.layout.linkbuds_widget
            val views = RemoteViews(context.packageName, layoutRes)

            views.setImageViewBitmap(R.id.widget_bg, buildBackground(context))

            val deviceName = WidgetPrefs.deviceName(context)
            val left = WidgetPrefs.left(context)
            val right = WidgetPrefs.right(context)
            val case = WidgetPrefs.case(context)
            val fallback = WidgetPrefs.fallbackLevel(context)
            val connected = WidgetPrefs.isConnected(context)
            val updatedAt = WidgetPrefs.updatedAt(context)

            views.setTextViewText(R.id.widget_device_name, deviceName ?: "Не выбрано")
            views.setTextViewText(R.id.widget_left, percentText(left))
            views.setTextViewText(R.id.widget_right, percentText(right))
            views.setTextViewText(R.id.widget_case, percentText(case))

            val hasSonyData = left != null || right != null || case != null
            if (!hasSonyData && fallback != null) {
                views.setViewVisibility(R.id.widget_fallback_note, View.VISIBLE)
                views.setTextViewText(R.id.widget_fallback_note, "Общий заряд: $fallback%")
            } else {
                views.setViewVisibility(R.id.widget_fallback_note, View.GONE)
            }

            views.setInt(
                R.id.widget_status_dot,
                "setColorFilter",
                if (connected) COLOR_CONNECTED else COLOR_DISCONNECTED
            )

            views.setTextViewText(
                R.id.widget_updated_at,
                if (updatedAt > 0) SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(updatedAt)) else ""
            )

            val refreshIntent = Intent(context, LinkBudsBatteryReceiver::class.java).apply { action = ACTION_REFRESH }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context, 0, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)

            if (expanded) {
                bindAncButtons(context, views, WidgetPrefs.ancMode(context))
            }

            return views
        }

        private fun bindAncButtons(context: Context, views: RemoteViews, currentMode: AncMode?) {
            bindAncButton(context, views, R.id.widget_anc_off, AncMode.OFF, currentMode, ANC_REQUEST_CODE_OFF)
            bindAncButton(context, views, R.id.widget_anc_nc, AncMode.NOISE_CANCELLING, currentMode, ANC_REQUEST_CODE_NC)
            bindAncButton(context, views, R.id.widget_anc_ambient, AncMode.AMBIENT_SOUND, currentMode, ANC_REQUEST_CODE_AMBIENT)
        }

        private fun bindAncButton(
            context: Context,
            views: RemoteViews,
            viewId: Int,
            mode: AncMode,
            currentMode: AncMode?,
            requestCode: Int
        ) {
            views.setInt(
                viewId,
                "setBackgroundResource",
                if (mode == currentMode) R.drawable.widget_anc_button_selected_bg else R.drawable.widget_anc_button_bg
            )

            val intent = Intent(context, LinkBudsBatteryReceiver::class.java).apply {
                action = ACTION_SET_ANC
                putExtra(EXTRA_ANC_MODE, mode.name)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(viewId, pendingIntent)
        }
    }
}
