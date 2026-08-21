package com.gesys.linkbuds_widget

import android.bluetooth.BluetoothDevice
import android.content.Context

/**
 * Single place that picks the right brand-specific [BatterySource], polls it, falls back to
 * the generic reflection API if it fails, and persists the result for the widget -- used by
 * the app's config screen, the background receiver, and the widget's own refresh button, so
 * there's one poll implementation instead of three.
 */
object BatteryPoller {
    data class PollResult(val left: Int?, val right: Int?, val case: Int?, val fallback: Int?) {
        fun isEmpty() = left == null && right == null && case == null && fallback == null
    }

    /** Blocking; must be called off the main thread. */
    fun pollAndSave(context: Context, device: BluetoothDevice, deviceName: String, connected: Boolean): PollResult {
        val source = BatterySources.forDeviceName(deviceName)
        val reading = source?.read(context, device)

        val fallback = if (reading == null || reading.isEmpty()) {
            BatteryReader.readReflection(device)
        } else {
            null
        }

        WidgetPrefs.saveStatus(
            context,
            left = reading?.left,
            right = reading?.right,
            case = reading?.case,
            fallbackLevel = fallback,
            connected = connected
        )

        // Separate connection from the battery read above -- these protocols don't share a
        // session between the two, so this is an extra RFCOMM round trip. Acceptable given how
        // infrequently polling happens (ACL events + a 15+ minute periodic fallback).
        val ancSource = AncControlSources.forDeviceName(deviceName)
        WidgetPrefs.saveAncMode(context, ancSource?.getMode(device))

        LinkBudsWidgetProvider.refreshAll(context)

        return PollResult(reading?.left, reading?.right, reading?.case, fallback)
    }
}
