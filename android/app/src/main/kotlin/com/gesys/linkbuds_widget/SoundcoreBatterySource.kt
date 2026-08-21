package com.gesys.linkbuds_widget

import android.bluetooth.BluetoothDevice
import android.content.Context

/**
 * Matches any device whose name contains "soundcore" -- the channel-sweep connection and
 * multi-layout battery extraction in [SoundcoreBatteryProtocol] were built for broad
 * compatibility across the Soundcore lineup (ported from a multi-model open source project,
 * not a single-model one), not just specific models. A wrong match just fails the channel
 * sweep and falls back to the reflection API, so matching broadly here is low-risk.
 */
class SoundcoreBatterySource : BatterySource {
    override fun matches(deviceName: String): Boolean = deviceName.contains("soundcore", ignoreCase = true)

    override fun read(context: Context, device: BluetoothDevice): BatteryReading? {
        val result = SoundcoreBatteryProtocol.read(device) ?: return null
        val reading = BatteryReading(result.left, result.right, result.case)
        return if (reading.isEmpty()) null else reading
    }
}
