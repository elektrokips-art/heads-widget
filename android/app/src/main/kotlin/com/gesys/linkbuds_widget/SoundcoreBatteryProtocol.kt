package com.gesys.linkbuds_widget

import android.bluetooth.BluetoothDevice

/**
 * Battery query over [SoundcoreConnection]. Battery rides inside the device-info response
 * (category=0x01, type=0x01) rather than a separate message -- extraction tries three layouts
 * in order (fixed offsets 40-42, a single-value "unified" block, then a marker-byte search)
 * because the offset has shifted between firmware versions; see CoreSound's device-protocol.js
 * for the btsnoop analysis this is ported from.
 */
object SoundcoreBatteryProtocol {
    private const val CATEGORY_DEVICE_INFO = 0x01
    private const val TYPE_DEVICE_INFO = 0x01

    data class Result(val left: Int?, val right: Int?, val case: Int?)

    fun read(device: BluetoothDevice, deviceName: String): Result? {
        val probe = SoundcoreConnection.encodeCommand(CATEGORY_DEVICE_INFO, TYPE_DEVICE_INFO, ByteArray(0))
        return SoundcoreConnection.withConnection(device, deviceName, probe) { _, _, confirmFrame ->
            // The probe *is* the device-info query, so the channel-confirmation frame already
            // is the response we need -- no further read required.
            if (confirmFrame.category != CATEGORY_DEVICE_INFO || confirmFrame.type != TYPE_DEVICE_INFO) {
                null
            } else {
                extractBattery(confirmFrame.payload)
            }
        }
    }

    private fun normalize(raw: Int): Int? {
        if (raw in 0..100) return raw
        return null // 0xFF/0xFE/0x7F etc. are sentinel/invalid values seen in some firmware states
    }

    private fun validCount(r: Result) = listOfNotNull(r.left, r.right, r.case).size

    private fun extractFixedOffsets(payload: ByteArray): Result? {
        if (payload.size < 43) return null
        val candidate = Result(
            left = normalize(payload[40].toInt() and 0xFF),
            right = normalize(payload[41].toInt() and 0xFF),
            case = normalize(payload[42].toInt() and 0xFF)
        )
        return if (validCount(candidate) >= 2) candidate else null
    }

    /** Some Liberty-family firmware exposes a single aggregate value in a tail config block
     *  instead of separate L/R/case: 02 00 <pct> 01 00 00 00 00 ff 00 00 01 ... */
    private fun extractUnified(payload: ByteArray): Result? {
        var i = 0
        while (i <= payload.size - 12) {
            if (payload[i] == 0x02.toByte() && payload[i + 1] == 0x00.toByte() &&
                payload[i + 3] == 0x01.toByte() && payload[i + 4] == 0x00.toByte() &&
                payload[i + 5] == 0x00.toByte() && payload[i + 6] == 0x00.toByte() &&
                payload[i + 7] == 0x00.toByte() && payload[i + 8] == 0xFF.toByte() &&
                payload[i + 9] == 0x00.toByte() && payload[i + 10] == 0x00.toByte() &&
                payload[i + 11] == 0x01.toByte()
            ) {
                val pct = normalize(payload[i + 2].toInt() and 0xFF)
                if (pct != null) return Result(left = null, right = null, case = pct)
            }
            i++
        }
        return null
    }

    private val MARKERS = listOf(
        byteArrayOf(0x01, 0xFE.toByte(), 0xFE.toByte()),
        byteArrayOf(0x02, 0x02, 0x00),
        byteArrayOf(0x01, 0xFF.toByte(), 0xFF.toByte()),
        byteArrayOf(0x00, 0xFE.toByte(), 0xFE.toByte())
    )

    private fun extractAfterMarkers(payload: ByteArray): Result? {
        var i = 0
        while (i <= payload.size - 6) {
            for (marker in MARKERS) {
                if (payload[i] == marker[0] && payload[i + 1] == marker[1] && payload[i + 2] == marker[2]) {
                    val candidate = Result(
                        left = normalize(payload[i + 3].toInt() and 0xFF),
                        right = normalize(payload[i + 4].toInt() and 0xFF),
                        case = normalize(payload[i + 5].toInt() and 0xFF)
                    )
                    if (validCount(candidate) >= 2) return candidate
                }
            }
            i++
        }
        return null
    }

    private fun extractBattery(payload: ByteArray): Result? {
        if (payload.size < 3) return null
        return extractFixedOffsets(payload) ?: extractUnified(payload) ?: extractAfterMarkers(payload)
    }
}
