package com.gesys.linkbuds_widget

import android.bluetooth.BluetoothDevice

/** Common subset of ANC modes across brands -- Sony/Soundcore each support finer-grained knobs
 *  (level, adaptive strength, wind reduction) that aren't exposed here to keep one control
 *  surface across brands; can be extended later if wanted. */
enum class AncMode { OFF, NOISE_CANCELLING, AMBIENT_SOUND }

interface AncControlSource {
    fun matches(deviceName: String): Boolean
    fun getMode(device: BluetoothDevice): AncMode?
    fun setMode(device: BluetoothDevice, mode: AncMode): Boolean
}

/** Only brands with a known ANC command are listed -- BBK's protocol (Oppo/Realme/OnePlus/vivo)
 *  has no ANC command in Gadgetbridge's implementation, and Apple's AirPods control protocol is
 *  a different, encrypted proprietary system this app doesn't implement (only passive battery
 *  beacon scanning). Unmatched brands simply get no ANC UI, rather than a fabricated attempt. */
object AncControlSources {
    private val sources: List<AncControlSource> = listOf(
        SonyAncSource(),
        SoundcoreAncSource(),
    )

    fun forDeviceName(deviceName: String): AncControlSource? = sources.firstOrNull { it.matches(deviceName) }
}

class SonyAncSource : AncControlSource {
    override fun matches(deviceName: String): Boolean = deviceName.contains("LinkBuds S", ignoreCase = true)
    override fun getMode(device: BluetoothDevice): AncMode? = SonyAncControl.getMode(device)
    override fun setMode(device: BluetoothDevice, mode: AncMode): Boolean = SonyAncControl.setMode(device, mode)
}

class SoundcoreAncSource : AncControlSource {
    // Matches any known model UUID, same as SoundcoreBatterySource -- see
    // SoundcoreAncControl's doc comment for why this is still the shakiest control path in the
    // app (mode-code mapping isn't confirmed stable across Soundcore firmware).
    override fun matches(deviceName: String): Boolean = SoundcoreModels.uuidForDeviceName(deviceName) != null

    override fun getMode(device: BluetoothDevice): AncMode? =
        SoundcoreAncControl.getMode(device, device.name ?: return null)

    override fun setMode(device: BluetoothDevice, mode: AncMode): Boolean =
        SoundcoreAncControl.setMode(device, device.name ?: return false, mode)
}
