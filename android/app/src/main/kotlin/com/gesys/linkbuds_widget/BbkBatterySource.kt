package com.gesys.linkbuds_widget

import android.bluetooth.BluetoothDevice
import android.content.Context
import java.util.Locale

/**
 * OPPO Enco / Realme Buds are confirmed via Gadgetbridge. OnePlus Buds and vivo TWS models
 * are matched too since they're the same BBK Electronics hardware family and this costs
 * nothing to try -- a wrong guess just fails the RFCOMM connect/parse and falls back to the
 * reflection API, it doesn't produce bad data.
 */
class BbkBatterySource : BatterySource {
    override fun matches(deviceName: String): Boolean {
        val lower = deviceName.lowercase(Locale.ROOT)
        return lower.contains("enco") ||
            lower.contains("realme buds") ||
            lower.contains("oneplus buds") ||
            lower.contains("vivo tws") ||
            lower.contains("iqoo")
    }

    override fun read(context: Context, device: BluetoothDevice): BatteryReading? {
        val result = BbkBatteryProtocol.read(device) ?: return null
        val reading = BatteryReading(result.left, result.right, result.case)
        return if (reading.isEmpty()) null else reading
    }
}
