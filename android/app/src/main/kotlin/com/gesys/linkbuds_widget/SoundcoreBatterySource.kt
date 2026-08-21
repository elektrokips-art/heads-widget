package com.gesys.linkbuds_widget

import android.bluetooth.BluetoothDevice
import android.content.Context

/**
 * Matches any device with a known Soundcore model UUID (see [SoundcoreModels], extracted from
 * the official app's own resources) -- covers the whole current earbuds/headphones lineup, not
 * just the couple of models this was originally hand-verified against.
 */
class SoundcoreBatterySource : BatterySource {
    override fun matches(deviceName: String): Boolean = SoundcoreModels.uuidForDeviceName(deviceName) != null

    override fun read(context: Context, device: BluetoothDevice): BatteryReading? {
        val result = SoundcoreBatteryProtocol.read(device, device.name ?: return null) ?: return null
        val reading = BatteryReading(result.left, result.right, result.case)
        return if (reading.isEmpty()) null else reading
    }
}
