package com.gesys.linkbuds_widget

import android.bluetooth.BluetoothDevice
import android.util.Log

/**
 * Fallback battery source: the single combined percentage Android's own Bluetooth stack
 * already tracks for a device (shown in system Bluetooth settings), read via the hidden,
 * non-SDK BluetoothDevice.getBatteryLevel(). Confirmed working on real LinkBuds S hardware,
 * but only reports one value -- not left/right/case. [SonyBatteryProtocol] is the primary
 * source for the split values; this is the fallback when that fails.
 */
object BatteryReader {
    private const val TAG = "BatteryReader"

    fun readReflection(device: BluetoothDevice): Int? {
        return try {
            val method = BluetoothDevice::class.java.getMethod("getBatteryLevel")
            val level = method.invoke(device) as? Int
            if (level == null || level < 0) null else level
        } catch (e: Exception) {
            Log.w(TAG, "getBatteryLevel() unavailable: ${e.javaClass.simpleName}")
            null
        }
    }
}
