package com.gesys.linkbuds_widget

import android.bluetooth.BluetoothDevice

/**
 * ANC control over [SonyHeadphonesSession] -- scoped to LinkBuds S specifically. The 7-byte
 * payload layout (selector 0x15, no wind-noise-reduction slot) matches devices without the
 * AmbientSoundControl2/WindNoiseReduction capabilities per Gadgetbridge's coordinator data for
 * this model; other Sony models use an 8-byte variant with an extra byte, not implemented here.
 */
object SonyAncControl {
    private const val PAYLOAD_ANC_GET: Byte = 0x66
    private const val PAYLOAD_ANC_RET: Byte = 0x67
    private const val PAYLOAD_ANC_SET: Byte = 0x68
    private const val PAYLOAD_ANC_NOTIFY: Byte = 0x69
    private const val SELECTOR: Byte = 0x15

    private val EXPECTED_REPLY_TYPES = setOf(PAYLOAD_ANC_RET, PAYLOAD_ANC_NOTIFY)

    fun getMode(device: BluetoothDevice, timeoutMs: Long = 8000): AncMode? {
        return SonyHeadphonesSession.withSession(device, timeoutMs) { session ->
            val payload = session.request(byteArrayOf(PAYLOAD_ANC_GET, SELECTOR), EXPECTED_REPLY_TYPES)
                ?: return@withSession null
            parseMode(payload)
        }
    }

    fun setMode(device: BluetoothDevice, mode: AncMode, timeoutMs: Long = 8000): Boolean {
        val result = SonyHeadphonesSession.withSession(device, timeoutMs) { session ->
            session.request(encodeSet(mode), EXPECTED_REPLY_TYPES)
        }
        return result != null
    }

    private fun encodeSet(mode: AncMode): ByteArray {
        val enabled: Byte = if (mode == AncMode.OFF) 0x00 else 0x01
        val ambient: Byte = if (mode == AncMode.AMBIENT_SOUND) 0x01 else 0x00
        // focusOnVoice=off, ambient level=10 (mid of the 1-20 range) -- not exposed in our UI
        return byteArrayOf(PAYLOAD_ANC_SET, SELECTOR, 0x01, enabled, ambient, 0x00, 10)
    }

    /** payload: [type][selector][reserved][enabled][ambient/nc][focusOnVoice][level] */
    private fun parseMode(payload: ByteArray): AncMode? {
        if (payload.size < 7) return null
        if (payload[3] == 0x00.toByte()) return AncMode.OFF
        if (payload[3] != 0x01.toByte()) return null
        return when (payload[4]) {
            0x00.toByte() -> AncMode.NOISE_CANCELLING
            0x01.toByte() -> AncMode.AMBIENT_SOUND
            else -> null
        }
    }
}
