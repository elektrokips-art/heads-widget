package com.gesys.linkbuds_widget

import android.bluetooth.BluetoothDevice

/**
 * ANC control over [SoundcoreConnection] (category 0x06). CoreSound's own source explicitly
 * notes the mode-code mapping "can vary across Soundcore firmware families" -- the values below
 * are theirs for the firmware they captured (0x00=ANC, 0x01=Transparency, 0x02=Normal), inverted
 * from an older mapping they also saw. So unlike Sony's ANC (protocol-confirmed on real
 * hardware), this is the least-verified control path in the app: if it's inverted on a given
 * unit, tapping "Ambient" could turn NC on instead. Kept in because a wrong toggle is just
 * annoying, not harmful, and the battery-side of this same protocol is what's actually confirmed.
 */
object SoundcoreAncControl {
    private const val CATEGORY_MODE = 0x06
    private const val TYPE_MODE_GET = 0x01
    private const val TYPE_MODE_SET = 0x81

    private const val MODE_ANC = 0x00
    private const val MODE_TRANSPARENCY = 0x01
    private const val MODE_NORMAL = 0x02

    private const val ANC_SCENE_LEVEL5 = 0x5
    private const val TRANSPARENCY_SCENE_VOCAL = 0x1

    private fun deviceInfoProbe() = SoundcoreConnection.encodeCommand(0x01, 0x01, ByteArray(0))

    fun getMode(device: BluetoothDevice, deviceName: String): AncMode? {
        return SoundcoreConnection.withConnection(device, deviceName, deviceInfoProbe()) { input, output, _ ->
            output.write(SoundcoreConnection.encodeCommand(CATEGORY_MODE, TYPE_MODE_GET, ByteArray(0)))
            output.flush()

            var mode: AncMode? = null
            val deadline = System.currentTimeMillis() + 3000
            while (mode == null && System.currentTimeMillis() < deadline) {
                val frame = SoundcoreConnection.readFrame(input, 3000) ?: break
                if (frame.category == CATEGORY_MODE && frame.payload.isNotEmpty()) {
                    mode = decodeMode(frame.payload[0].toInt() and 0xFF)
                }
            }
            mode
        }
    }

    fun setMode(device: BluetoothDevice, deviceName: String, mode: AncMode): Boolean {
        val modeByte = when (mode) {
            AncMode.NOISE_CANCELLING -> MODE_ANC
            AncMode.AMBIENT_SOUND -> MODE_TRANSPARENCY
            AncMode.OFF -> MODE_NORMAL
        }
        val sceneByte = (ANC_SCENE_LEVEL5 shl 4) or TRANSPARENCY_SCENE_VOCAL
        val payload = byteArrayOf(modeByte.toByte(), sceneByte.toByte(), 0x01, 0x00, 0x00, 0x00, 0x03)
        val command = SoundcoreConnection.encodeCommand(CATEGORY_MODE, TYPE_MODE_SET, payload)

        // Confirm the channel with the device-info probe (guaranteed a reply), then send the
        // actual mode-set command -- the earbuds don't necessarily ack a mode change, so using
        // the set command itself as the confirmation probe would misidentify a good channel as
        // silent.
        val sent = SoundcoreConnection.withConnection(device, deviceName, deviceInfoProbe()) { _, output, _ ->
            output.write(command)
            output.flush()
            true
        }
        return sent == true
    }

    private fun decodeMode(raw: Int): AncMode? = when (raw) {
        MODE_ANC -> AncMode.NOISE_CANCELLING
        MODE_TRANSPARENCY -> AncMode.AMBIENT_SOUND
        MODE_NORMAL -> AncMode.OFF
        else -> null
    }
}
