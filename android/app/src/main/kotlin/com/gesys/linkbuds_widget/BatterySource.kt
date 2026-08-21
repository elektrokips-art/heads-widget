package com.gesys.linkbuds_widget

import android.bluetooth.BluetoothDevice
import android.content.Context

/** One battery reading; a device may report anywhere from one to three of these. */
data class BatteryReading(val left: Int?, val right: Int?, val case: Int?) {
    fun isEmpty() = left == null && right == null && case == null
}

/** A brand-specific way of reading battery for a paired device. */
interface BatterySource {
    /** Whether this source knows how to talk to a device with this (Bluetooth) name. */
    fun matches(deviceName: String): Boolean

    /** Blocking; must be called off the main thread. Null means this attempt failed. */
    fun read(context: Context, device: BluetoothDevice): BatteryReading?
}

/** Picks the right [BatterySource] by device name, mirroring how Gadgetbridge's device
 *  coordinators are matched by name pattern -- add a new source here per brand. */
object BatterySources {
    private val sources: List<BatterySource> = listOf(
        SonyBatterySource(),
        AppleBatterySource(),
        BbkBatterySource(),
        SoundcoreBatterySource(),
    )

    fun forDeviceName(deviceName: String): BatterySource? = sources.firstOrNull { it.matches(deviceName) }
}
