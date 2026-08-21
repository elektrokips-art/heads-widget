package com.gesys.linkbuds_widget

import android.content.Context

/** Shared between the app UI, the background receiver, and the widget provider. */
object WidgetPrefs {
    private const val PREFS_NAME = "linkbuds_widget_prefs"

    private const val KEY_DEVICE_ADDRESS = "device_address"
    private const val KEY_DEVICE_NAME = "device_name"
    private const val KEY_LEFT = "battery_left"
    private const val KEY_RIGHT = "battery_right"
    private const val KEY_CASE = "battery_case"
    private const val KEY_FALLBACK_LEVEL = "battery_fallback"
    private const val KEY_CONNECTED = "connected"
    private const val KEY_UPDATED_AT = "updated_at"
    private const val KEY_ANC_MODE = "anc_mode"

    private const val KEY_COLOR = "widget_color"
    private const val KEY_OPACITY_PERCENT = "widget_opacity_percent"
    private const val KEY_CORNER_RADIUS_DP = "widget_corner_radius_dp"
    private const val KEY_REFRESH_INTERVAL_MIN = "widget_refresh_interval_min"

    const val DEFAULT_COLOR = 0xFF4527A0.toInt()
    const val DEFAULT_OPACITY_PERCENT = 100
    const val DEFAULT_CORNER_RADIUS_DP = 16
    const val DEFAULT_REFRESH_INTERVAL_MIN = 30

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveDevice(context: Context, address: String, name: String) {
        prefs(context).edit()
            .putString(KEY_DEVICE_ADDRESS, address)
            .putString(KEY_DEVICE_NAME, name)
            .apply()
    }

    fun deviceAddress(context: Context): String? = prefs(context).getString(KEY_DEVICE_ADDRESS, null)
    fun deviceName(context: Context): String? = prefs(context).getString(KEY_DEVICE_NAME, null)

    /**
     * [left]/[right]/[case] from the Sony protocol; [fallbackLevel] from the reflection API,
     * used by the widget only when all three of the above are null.
     */
    fun saveStatus(
        context: Context,
        left: Int?,
        right: Int?,
        case: Int?,
        fallbackLevel: Int?,
        connected: Boolean
    ) {
        prefs(context).edit()
            .putInt(KEY_LEFT, left ?: -1)
            .putInt(KEY_RIGHT, right ?: -1)
            .putInt(KEY_CASE, case ?: -1)
            .putInt(KEY_FALLBACK_LEVEL, fallbackLevel ?: -1)
            .putBoolean(KEY_CONNECTED, connected)
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .apply()
    }

    fun setConnected(context: Context, connected: Boolean) {
        prefs(context).edit().putBoolean(KEY_CONNECTED, connected).apply()
    }

    private fun readOrNull(context: Context, key: String): Int? {
        val v = prefs(context).getInt(key, -1)
        return if (v < 0) null else v
    }

    fun left(context: Context): Int? = readOrNull(context, KEY_LEFT)
    fun right(context: Context): Int? = readOrNull(context, KEY_RIGHT)
    fun case(context: Context): Int? = readOrNull(context, KEY_CASE)
    fun fallbackLevel(context: Context): Int? = readOrNull(context, KEY_FALLBACK_LEVEL)
    fun isConnected(context: Context): Boolean = prefs(context).getBoolean(KEY_CONNECTED, false)
    fun updatedAt(context: Context): Long = prefs(context).getLong(KEY_UPDATED_AT, 0L)

    /** Null when the device's brand has no known ANC control, or it hasn't been read yet. */
    fun saveAncMode(context: Context, mode: AncMode?) {
        prefs(context).edit().putString(KEY_ANC_MODE, mode?.name).apply()
    }

    fun ancMode(context: Context): AncMode? {
        val name = prefs(context).getString(KEY_ANC_MODE, null) ?: return null
        return try {
            AncMode.valueOf(name)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun saveAppearance(
        context: Context,
        color: Int,
        opacityPercent: Int,
        cornerRadiusDp: Int,
        refreshIntervalMin: Int
    ) {
        prefs(context).edit()
            .putInt(KEY_COLOR, color)
            .putInt(KEY_OPACITY_PERCENT, opacityPercent)
            .putInt(KEY_CORNER_RADIUS_DP, cornerRadiusDp)
            .putInt(KEY_REFRESH_INTERVAL_MIN, refreshIntervalMin)
            .apply()
    }

    fun color(context: Context): Int = prefs(context).getInt(KEY_COLOR, DEFAULT_COLOR)
    fun opacityPercent(context: Context): Int = prefs(context).getInt(KEY_OPACITY_PERCENT, DEFAULT_OPACITY_PERCENT)
    fun cornerRadiusDp(context: Context): Int = prefs(context).getInt(KEY_CORNER_RADIUS_DP, DEFAULT_CORNER_RADIUS_DP)
    fun refreshIntervalMin(context: Context): Int =
        prefs(context).getInt(KEY_REFRESH_INTERVAL_MIN, DEFAULT_REFRESH_INTERVAL_MIN)
}
