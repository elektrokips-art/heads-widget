package com.gesys.linkbuds_widget

import android.bluetooth.BluetoothDevice
import android.content.Context

/**
 * Only LinkBuds S is wired up -- that's the model this was actually written and tested
 * against. Other Sony models (WF-1000XM*, WH-1000XM*, plain LinkBuds, ...) use the same
 * message framing but different battery-type codes and, for some, protocol V1 instead of
 * V2 -- matching them here without verifying each one would risk silently showing wrong
 * percentages, so they're left unmatched until confirmed.
 */
class SonyBatterySource : BatterySource {
    override fun matches(deviceName: String): Boolean = deviceName.contains("LinkBuds S", ignoreCase = true)

    override fun read(context: Context, device: BluetoothDevice): BatteryReading? {
        val result = SonyBatteryProtocol.read(device) ?: return null
        val reading = BatteryReading(result.left, result.right, result.case)
        return if (reading.isEmpty()) null else reading
    }
}
